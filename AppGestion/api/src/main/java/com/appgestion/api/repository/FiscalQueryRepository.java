package com.appgestion.api.repository;

import com.appgestion.api.domain.entity.Factura;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas nativas de agregación fiscal (303 / 347). Sin CRUD genérico.
 */
public interface FiscalQueryRepository extends Repository<Factura, Long> {

    @Query(value = """
            SELECT CAST(COALESCE(SUM(f.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(f.iva), 0) AS numeric),
                   COUNT(f.id)
            FROM facturas f
            WHERE f.usuario_id = :usuarioId
              AND COALESCE(f.fecha_expedicion, f.fecha_operacion, CAST(f.fecha_creacion AS date)) BETWEEN :desde AND :hasta
              AND (NOT :soloPagadas OR TRIM(f.estado_pago) = :estadoPagada)
            """, nativeQuery = true)
    Object[] aggregateVentasModelo303Devengo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("soloPagadas") boolean soloPagadas,
            @Param("estadoPagada") String estadoPagada
    );

    @Query(value = """
            SELECT CAST(COALESCE(SUM(f.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(f.iva), 0) AS numeric),
                   COUNT(DISTINCT f.id)
            FROM facturas f
            INNER JOIN (
                SELECT factura_id, MAX(fecha) AS ultima_fecha
                FROM factura_cobros
                GROUP BY factura_id
            ) cob ON cob.factura_id = f.id
            WHERE f.usuario_id = :usuarioId
              AND TRIM(f.estado_pago) = :estadoPagada
              AND cob.ultima_fecha BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    Object[] aggregateVentasModelo303Caja(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estadoPagada") String estadoPagada
    );

    @Query(value = """
            SELECT c.id, c.nombre, c.dni, CAST(COALESCE(SUM(f.subtotal), 0) AS numeric)
            FROM facturas f
            INNER JOIN clientes c ON c.id = f.cliente_id
            WHERE f.usuario_id = :usuarioId
              AND COALESCE(f.fecha_expedicion, f.fecha_operacion, CAST(f.fecha_creacion AS date)) BETWEEN :desde AND :hasta
              AND c.dni IS NOT NULL
              AND TRIM(c.dni) <> ''
            GROUP BY c.id, c.nombre, c.dni
            HAVING COALESCE(SUM(f.subtotal), 0) > :umbral
            ORDER BY 4 DESC
            """, nativeQuery = true)
    List<Object[]> findClientesBaseAnualSuperaUmbral347(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("umbral") double umbral
    );

    @Query(value = """
            SELECT CAST(COALESCE(SUM(g.cuota_iva), 0) AS numeric)
            FROM gastos g
            WHERE g.usuario_id = :usuarioId
              AND g.fecha BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    Object aggregateGastosModelo303Devengo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    /**
     * Gastos con el valor de API {@code CAJA} (en pantalla, «Por fecha de cobro»).
     * El IVA soportado sigue la fecha del gasto: las compras no tienen fecha de pago.
     */
    @Query(value = """
            SELECT CAST(COALESCE(SUM(g.cuota_iva), 0) AS numeric)
            FROM gastos g
            WHERE g.usuario_id = :usuarioId
              AND g.fecha BETWEEN :desde AND :hasta
            """, nativeQuery = true)
    Object aggregateGastosModelo303Caja(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    /**
     * Cabecera de cada factura del trimestre (devengo) junto a la suma de sus líneas,
     * para detectar descuadres. Mismos filtros que {@link #aggregateVentasModelo303Devengo}.
     */
    @Query(value = """
            SELECT f.id,
                   f.numero_factura,
                   f.cliente_id,
                   CAST(COALESCE(f.subtotal, 0) AS numeric),
                   CAST(COALESCE(f.iva, 0) AS numeric),
                   CAST(COALESCE(SUM(i.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(COALESCE(i.cuota_iva, 0)), 0) AS numeric),
                   COUNT(i.id)
            FROM facturas f
            LEFT JOIN factura_items i ON i.factura_id = f.id
            WHERE f.usuario_id = :usuarioId
              AND COALESCE(f.fecha_expedicion, f.fecha_operacion, CAST(f.fecha_creacion AS date)) BETWEEN :desde AND :hasta
              AND (NOT :soloPagadas OR TRIM(f.estado_pago) = :estadoPagada)
            GROUP BY f.id, f.numero_factura, f.cliente_id, f.subtotal, f.iva
            ORDER BY f.id
            """, nativeQuery = true)
    List<Object[]> findFacturasCalidadDevengo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("soloPagadas") boolean soloPagadas,
            @Param("estadoPagada") String estadoPagada
    );

    /**
     * Igual que {@link #findFacturasCalidadDevengo} con el filtro de último cobro del criterio por fecha de cobro.
     */
    @Query(value = """
            SELECT f.id,
                   f.numero_factura,
                   f.cliente_id,
                   CAST(COALESCE(f.subtotal, 0) AS numeric),
                   CAST(COALESCE(f.iva, 0) AS numeric),
                   CAST(COALESCE(SUM(i.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(COALESCE(i.cuota_iva, 0)), 0) AS numeric),
                   COUNT(i.id)
            FROM facturas f
            INNER JOIN (
                SELECT factura_id, MAX(fecha) AS ultima_fecha
                FROM factura_cobros
                GROUP BY factura_id
            ) cob ON cob.factura_id = f.id
            LEFT JOIN factura_items i ON i.factura_id = f.id
            WHERE f.usuario_id = :usuarioId
              AND TRIM(f.estado_pago) = :estadoPagada
              AND cob.ultima_fecha BETWEEN :desde AND :hasta
            GROUP BY f.id, f.numero_factura, f.cliente_id, f.subtotal, f.iva
            ORDER BY f.id
            """, nativeQuery = true)
    List<Object[]> findFacturasCalidadCaja(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estadoPagada") String estadoPagada
    );

    /**
     * Bases y cuotas de línea del trimestre (devengo), agrupadas por tipo de IVA.
     * Mismos filtros que {@link #aggregateVentasModelo303Devengo}.
     */
    @Query(value = """
            SELECT i.tipo_iva,
                   CAST(COALESCE(SUM(i.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(COALESCE(i.cuota_iva, 0)), 0) AS numeric)
            FROM facturas f
            INNER JOIN factura_items i ON i.factura_id = f.id
            WHERE f.usuario_id = :usuarioId
              AND COALESCE(f.fecha_expedicion, f.fecha_operacion, CAST(f.fecha_creacion AS date)) BETWEEN :desde AND :hasta
              AND (NOT :soloPagadas OR TRIM(f.estado_pago) = :estadoPagada)
            GROUP BY i.tipo_iva
            ORDER BY i.tipo_iva DESC
            """, nativeQuery = true)
    List<Object[]> findDesgloseIvaDevengo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("soloPagadas") boolean soloPagadas,
            @Param("estadoPagada") String estadoPagada
    );

    /**
     * Igual que {@link #findDesgloseIvaDevengo} con el filtro de último cobro (criterio por fecha de cobro).
     */
    @Query(value = """
            SELECT i.tipo_iva,
                   CAST(COALESCE(SUM(i.subtotal), 0) AS numeric),
                   CAST(COALESCE(SUM(COALESCE(i.cuota_iva, 0)), 0) AS numeric)
            FROM facturas f
            INNER JOIN (
                SELECT factura_id, MAX(fecha) AS ultima_fecha
                FROM factura_cobros
                GROUP BY factura_id
            ) cob ON cob.factura_id = f.id
            INNER JOIN factura_items i ON i.factura_id = f.id
            WHERE f.usuario_id = :usuarioId
              AND TRIM(f.estado_pago) = :estadoPagada
              AND cob.ultima_fecha BETWEEN :desde AND :hasta
            GROUP BY i.tipo_iva
            ORDER BY i.tipo_iva DESC
            """, nativeQuery = true)
    List<Object[]> findDesgloseIvaCaja(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estadoPagada") String estadoPagada
    );

    /**
     * Gastos del trimestre y, si los hubiera, gastos sin fecha (no entran en ningún trimestre).
     */
    @Query(value = """
            SELECT g.id, g.fecha, g.categoria, g.proveedor
            FROM gastos g
            WHERE g.usuario_id = :usuarioId
              AND (g.fecha BETWEEN :desde AND :hasta OR g.fecha IS NULL)
            ORDER BY g.id
            """, nativeQuery = true)
    List<Object[]> findGastosCalidad(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );
}
