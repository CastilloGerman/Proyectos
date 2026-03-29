package com.appgestion.api.service;

import com.appgestion.api.domain.enums.FiscalCriterioImputacion;
import com.appgestion.api.dto.response.Modelo303ResumenResponse;
import com.appgestion.api.dto.response.Modelo347ClienteResponse;
import com.appgestion.api.dto.response.Modelo347ResumenResponse;
import com.appgestion.api.repository.FiscalQueryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FiscalService {

    /** Umbral orientativo anual por contraparte para el modelo 347 (€). */
    public static final BigDecimal UMBRAL_MODELO_347 = new BigDecimal("3005.06");

    private static final String AVISO_LEGAL_303 = """
            Este resumen es informativo y se calcula a partir de las facturas registradas en Noemí. \
            No sustituye el Modelo 303 oficial ni la presentación telemática en la AEAT. \
            Consulta con tu gestor o en la sede electrónica para obligaciones tributarias concretas.""";

    private static final String AVISO_LEGAL_347 = """
            Listado orientativo de clientes cuya base imponible anual (facturas emitidas en la app, criterio devengo) \
            supera 3.005,06 €. No sustituye el Modelo 347 ni su presentación. Verifica datos fiscales y exclusiones en normativa vigente.""";

    private static final String NOTA_IVA_SOPORTADO = """
            El IVA soportado (compras/gastos) no está disponible en la aplicación en esta versión; se muestra 0 €. \
            Cuando exista registro de compras, se podrá incluir en el cálculo.""";

    private final FiscalQueryRepository fiscalQueryRepository;

    public FiscalService(FiscalQueryRepository fiscalQueryRepository) {
        this.fiscalQueryRepository = fiscalQueryRepository;
    }

    public Modelo303ResumenResponse resumenModelo303(
            Long usuarioId,
            int anio,
            int trimestre,
            FiscalCriterioImputacion criterio,
            boolean soloFacturasPagadas
    ) {
        if (trimestre < 1 || trimestre > 4) {
            throw new IllegalArgumentException("El trimestre debe estar entre 1 y 4.");
        }
        if (anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException("Año fuera de rango permitido.");
        }

        LocalDate desde = fechaInicioTrimestre(anio, trimestre);
        LocalDate hasta = fechaFinTrimestre(anio, trimestre);

        Object[] row;
        List<String> advertencias = new ArrayList<>();

        boolean soloPagadasEnRespuesta = false;
        if (criterio == FiscalCriterioImputacion.CAJA) {
            row = fiscalQueryRepository.aggregateVentasModelo303Caja(usuarioId, desde, hasta);
            advertencias.add(
                    "Criterio caja: solo entran facturas marcadas como Pagada con al menos un cobro registrado; " +
                            "la fecha usada es la del último cobro (MAX) por factura."
            );
            advertencias.add(
                    "Factura de anticipo: al generarla se registra un cobro automático con la fecha del anticipo " +
                            "para que el criterio caja pueda imputar el IVA en el trimestre del cobro."
            );
        } else {
            soloPagadasEnRespuesta = soloFacturasPagadas;
            row = fiscalQueryRepository.aggregateVentasModelo303Devengo(usuarioId, desde, hasta, soloFacturasPagadas);
            if (soloFacturasPagadas) {
                advertencias.add("Filtro activo: solo facturas con estado Pagada (criterio devengo por fecha de expedición).");
            }
            advertencias.add(
                    "Facturas finales con anticipo: la base y el IVA mostrados son solo los del tramo remanente; " +
                            "el anticipo ya se contabilizó en la factura de anticipo (sin duplicar en el mismo trimestre si las fechas difieren)."
            );
        }

        row = unwrapNativeTupleRow(row);

        BigDecimal base = toMoney(row[0]);
        BigDecimal ivaRep = toMoney(row[1]);
        long numFacturas = row[2] != null ? ((Number) row[2]).longValue() : 0L;

        BigDecimal ivaSop = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal resultado = ivaRep.subtract(ivaSop).setScale(2, RoundingMode.HALF_UP);

        return new Modelo303ResumenResponse(
                anio,
                trimestre,
                desde,
                hasta,
                criterio,
                soloPagadasEnRespuesta,
                base,
                ivaRep,
                ivaSop,
                false,
                NOTA_IVA_SOPORTADO.trim(),
                resultado,
                resultado.compareTo(BigDecimal.ZERO) >= 0,
                numFacturas,
                List.copyOf(advertencias),
                AVISO_LEGAL_303.trim().replace("            ", " ")
        );
    }

    public Modelo347ResumenResponse resumenModelo347(Long usuarioId, int anio) {
        if (anio < 2000 || anio > 2100) {
            throw new IllegalArgumentException("Año fuera de rango permitido.");
        }
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);

        List<Object[]> rows = fiscalQueryRepository.findClientesBaseAnualSuperaUmbral347(
                usuarioId,
                desde,
                hasta,
                UMBRAL_MODELO_347.doubleValue()
        );

        List<Modelo347ClienteResponse> clientes = new ArrayList<>();
        for (Object[] rRaw : rows) {
            Object[] r = unwrapNativeTupleRow(rRaw);
            Long clienteId = ((Number) r[0]).longValue();
            String nombre = r[1] != null ? r[1].toString() : "";
            String dni = r[2] != null ? r[2].toString() : "";
            BigDecimal base = toMoney(r[3]);
            clientes.add(new Modelo347ClienteResponse(clienteId, nombre, dni, base));
        }

        return new Modelo347ResumenResponse(
                anio,
                List.copyOf(clientes),
                clientes.size(),
                UMBRAL_MODELO_347.toPlainString(),
                AVISO_LEGAL_347.trim().replace("            ", " ")
        );
    }

    public static LocalDate fechaInicioTrimestre(int anio, int trimestre) {
        int mes = switch (trimestre) {
            case 1 -> 1;
            case 2 -> 4;
            case 3 -> 7;
            case 4 -> 10;
            default -> throw new IllegalArgumentException("Trimestre inválido");
        };
        return LocalDate.of(anio, mes, 1);
    }

    public static LocalDate fechaFinTrimestre(int anio, int trimestre) {
        return switch (trimestre) {
            case 1 -> LocalDate.of(anio, 3, 31);
            case 2 -> LocalDate.of(anio, 6, 30);
            case 3 -> LocalDate.of(anio, 9, 30);
            case 4 -> LocalDate.of(anio, 12, 31);
            default -> throw new IllegalArgumentException("Trimestre inválido");
        };
    }

    private static BigDecimal toMoney(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal bd) {
            return bd.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number n) {
            if (value instanceof Double d && (d.isNaN() || d.isInfinite())) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(value.toString().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor monetario no numérico en agregación fiscal", ex);
        }
    }

    /**
     * Hibernate 6 / Spring Data puede devolver una fila nativa como {@code Object[]{ Object[]{ col0, col1, ... } }}.
     */
    private static Object[] unwrapNativeTupleRow(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] inner) {
            return inner;
        }
        return row;
    }

    public static FiscalCriterioImputacion parseCriterio(String raw) {
        if (raw == null || raw.isBlank()) {
            return FiscalCriterioImputacion.DEVENGO;
        }
        String u = raw.trim().toUpperCase();
        if ("CAJA".equals(u)) {
            return FiscalCriterioImputacion.CAJA;
        }
        if ("DEVENGO".equals(u)) {
            return FiscalCriterioImputacion.DEVENGO;
        }
        throw new IllegalArgumentException("criterio debe ser DEVENGO o CAJA");
    }
}
