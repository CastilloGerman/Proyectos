package appgestion.service;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para obtener estadísticas y métricas de presupuestos, facturas
 * y materiales. Equivalente a las funciones de métricas en la app Python.
 */
public class MetricasService {

    private static final Logger log = Logger.getLogger(MetricasService.class.getName());

    public static class StatsPresupuestos {
        public int totalEmitidos;
        public int pendientes;
        public int aprobados;
        public int rechazados;
        public double totalValorEmitidos;
        public double totalValorAprobados;
        public double totalValorPendientes;
        public double promedioPresupuesto;
        public double totalDescuentos;
    }

    public static class StatsFacturas {
        public int totalEmitidas;
        public int noPagadas;
        public int pagadas;
        public double totalFacturado;
        public double totalPendienteCobro;
        public double promedioFactura;
        public double totalDescuentos;
    }

    public static class TopCliente {
        public String clienteNombre;
        public int cantidadFacturas;
        public double totalPagado;
        public double promedioFactura;
    }

    public static class TopMaterial {
        public String materialNombre;
        public double ingresosTotal;
        public double cantidadTotal;
        public int vecesUsado;
    }

    public static class EvolucionMensual {
        public String mes;
        public double facturacionPagada;
        public double facturacionPendiente;
    }

    public static class FacturaVencida {
        public String numeroFactura;
        public String clienteNombre;
        public double total;
        public int diasVencidos;
    }

    public static class FacturaProxima {
        public String numeroFactura;
        public String clienteNombre;
        public double total;
        public int diasRestantes;
    }

    public StatsPresupuestos obtenerEstadisticasPresupuestos(String fechaInicio, String fechaFin) {
        StatsPresupuestos s = new StatsPresupuestos();
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (fechaInicio != null && !fechaInicio.isBlank()) {
            where.append(" AND DATE(p.fecha_creacion) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isBlank()) {
            where.append(" AND DATE(p.fecha_creacion) <= ?");
            params.add(fechaFin);
        }
        String whereSql = where.length() > 0 ? " WHERE " + where.substring(5) : "";

        String sqlTotal = "SELECT COUNT(*) as total FROM presupuestos p" + whereSql;
        String sqlEstados = """
                SELECT COALESCE(estado, 'Pendiente') as estado, COUNT(*) as cantidad
                FROM presupuestos p
                """ + whereSql + """
                 GROUP BY COALESCE(estado, 'Pendiente')
                """;
        String sqlValores = """
                SELECT COALESCE(SUM(total), 0) as total_valor,
                       COALESCE(AVG(total), 0) as promedio,
                       COALESCE(SUM(COALESCE(descuento_global_fijo, 0)), 0) as descuentos
                FROM presupuestos p
                """ + whereSql;
        String sqlValorEstados = """
                SELECT COALESCE(estado, 'Pendiente') as estado, COALESCE(SUM(total), 0) as valor
                FROM presupuestos p
                """ + whereSql + """
                 GROUP BY COALESCE(estado, 'Pendiente')
                """;

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) s.totalEmitidos = rs.getInt("total");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlEstados)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String estado = rs.getString("estado");
                        int cant = rs.getInt("cantidad");
                        switch (estado) {
                            case "Pendiente" -> s.pendientes = cant;
                            case "Aprobado" -> s.aprobados = cant;
                            case "Rechazado" -> s.rechazados = cant;
                            default -> { }
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlValores)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        s.totalValorEmitidos = rs.getDouble("total_valor");
                        s.promedioPresupuesto = rs.getDouble("promedio");
                        s.totalDescuentos = rs.getDouble("descuentos");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlValorEstados)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String estado = rs.getString("estado");
                        double v = rs.getDouble("valor");
                        switch (estado) {
                            case "Pendiente" -> s.totalValorPendientes = v;
                            case "Aprobado" -> s.totalValorAprobados = v;
                            default -> { }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error estadísticas presupuestos", e);
        }
        return s;
    }

    public double obtenerTasaConversionPresupuestos(String fechaInicio, String fechaFin) {
        StatsPresupuestos s = obtenerEstadisticasPresupuestos(fechaInicio, fechaFin);
        int total = s.pendientes + s.aprobados + s.rechazados;
        if (total == 0) return 0.0;
        return (s.aprobados * 100.0) / total;
    }

    public StatsFacturas obtenerEstadisticasFacturas(String fechaInicio, String fechaFin) {
        StatsFacturas s = new StatsFacturas();
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (fechaInicio != null && !fechaInicio.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) <= ?");
            params.add(fechaFin);
        }
        String whereSql = where.length() > 0 ? " WHERE " + where.substring(5) : "";

        String sqlTotal = "SELECT COUNT(*) as total FROM facturas f" + whereSql;
        String sqlEstados = """
                SELECT COALESCE(estado_pago, 'No Pagada') as estado, COUNT(*) as cantidad
                FROM facturas f
                """ + whereSql + """
                 GROUP BY COALESCE(estado_pago, 'No Pagada')
                """;
        String sqlValores = """
                SELECT
                    COALESCE(SUM(CASE WHEN estado_pago = 'Pagada' THEN total ELSE 0 END), 0) as total_facturado,
                    COALESCE(SUM(CASE WHEN estado_pago = 'No Pagada' THEN total ELSE 0 END), 0) as pendiente,
                    COALESCE(AVG(total), 0) as promedio,
                    COALESCE(SUM(COALESCE(descuento_global_fijo, 0)), 0) as descuentos
                FROM facturas f
                """ + whereSql;

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) s.totalEmitidas = rs.getInt("total");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlEstados)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String estado = rs.getString("estado");
                        int cant = rs.getInt("cantidad");
                        switch (estado) {
                            case "No Pagada" -> s.noPagadas = cant;
                            case "Pagada" -> s.pagadas = cant;
                            default -> { }
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlValores)) {
                setParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        s.totalFacturado = rs.getDouble("total_facturado");
                        s.totalPendienteCobro = rs.getDouble("pendiente");
                        s.promedioFactura = rs.getDouble("promedio");
                        s.totalDescuentos = rs.getDouble("descuentos");
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error estadísticas facturas", e);
        }
        return s;
    }

    public double obtenerDiasPromedioCobro(String fechaInicio, String fechaFin) {
        StringBuilder where = new StringBuilder(" WHERE f.estado_pago = 'Pagada'");
        List<Object> params = new ArrayList<>();
        if (fechaInicio != null && !fechaInicio.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) <= ?");
            params.add(fechaFin);
        }
        String sql = "SELECT AVG(julianday('now') - julianday(f.fecha_creacion)) as dias FROM facturas f" + where;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double d = rs.getDouble("dias");
                    return Double.isNaN(d) ? 0 : d;
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error días promedio cobro", e);
        }
        return 0.0;
    }

    public List<FacturaVencida> obtenerFacturasVencidas() {
        String sql = """
                SELECT f.numero_factura, c.nombre as cliente_nombre, f.total,
                       CAST(julianday('now') - julianday(f.fecha_vencimiento) AS INTEGER) as dias_vencidos
                FROM facturas f
                JOIN clientes c ON f.cliente_id = c.id
                WHERE f.fecha_vencimiento IS NOT NULL AND DATE(f.fecha_vencimiento) < DATE('now')
                  AND f.estado_pago = 'No Pagada'
                ORDER BY f.fecha_vencimiento ASC
                """;
        List<FacturaVencida> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaVencida f = new FacturaVencida();
                f.numeroFactura = rs.getString("numero_factura");
                f.clienteNombre = rs.getString("cliente_nombre");
                f.total = rs.getDouble("total");
                f.diasVencidos = rs.getInt("dias_vencidos");
                lista.add(f);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error facturas vencidas", e);
        }
        return lista;
    }

    public List<FacturaProxima> obtenerFacturasProximasVencer(int dias) {
        String fechaLimite = LocalDate.now().plusDays(dias).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sql = """
                SELECT f.numero_factura, c.nombre as cliente_nombre, f.total,
                       CAST(julianday(f.fecha_vencimiento) - julianday('now') AS INTEGER) as dias_restantes
                FROM facturas f
                JOIN clientes c ON f.cliente_id = c.id
                WHERE f.fecha_vencimiento IS NOT NULL
                  AND DATE(f.fecha_vencimiento) >= DATE('now')
                  AND DATE(f.fecha_vencimiento) <= DATE(?)
                  AND f.estado_pago = 'No Pagada'
                ORDER BY f.fecha_vencimiento ASC
                """;
        List<FacturaProxima> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaLimite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaProxima f = new FacturaProxima();
                    f.numeroFactura = rs.getString("numero_factura");
                    f.clienteNombre = rs.getString("cliente_nombre");
                    f.total = rs.getDouble("total");
                    f.diasRestantes = rs.getInt("dias_restantes");
                    lista.add(f);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error facturas próximas", e);
        }
        return lista;
    }

    public List<TopCliente> obtenerTopClientes(String fechaInicio, String fechaFin, int limite) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (fechaInicio != null && !fechaInicio.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) <= ?");
            params.add(fechaFin);
        }
        String whereSql = where.length() > 0 ? " WHERE " + where.substring(5) : "";
        String sql = """
                SELECT c.nombre as cliente_nombre, COUNT(f.id) as cantidad_facturas,
                       COALESCE(SUM(CASE WHEN f.estado_pago = 'Pagada' THEN f.total ELSE 0 END), 0) as total_pagado,
                       COALESCE(AVG(f.total), 0) as promedio_factura
                FROM facturas f JOIN clientes c ON f.cliente_id = c.id
                """ + whereSql + """
                 GROUP BY c.id, c.nombre ORDER BY total_pagado DESC LIMIT ?
                """;
        params.add(limite);
        List<TopCliente> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TopCliente c = new TopCliente();
                    c.clienteNombre = rs.getString("cliente_nombre");
                    c.cantidadFacturas = rs.getInt("cantidad_facturas");
                    c.totalPagado = rs.getDouble("total_pagado");
                    c.promedioFactura = rs.getDouble("promedio_factura");
                    lista.add(c);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error top clientes", e);
        }
        return lista;
    }

    public List<TopMaterial> obtenerTopMaterialesPorIngresos(String fechaInicio, String fechaFin, int limite) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (fechaInicio != null && !fechaInicio.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) >= ?");
            params.add(fechaInicio);
        }
        if (fechaFin != null && !fechaFin.isBlank()) {
            where.append(" AND DATE(f.fecha_creacion) <= ?");
            params.add(fechaFin);
        }
        String whereSql = where.length() > 0 ? " WHERE " + where.substring(5) : "";
        String sql = """
                SELECT m.nombre as material_nombre, COALESCE(SUM(fi.subtotal), 0) as ingresos_total,
                       COALESCE(SUM(fi.cantidad), 0) as cantidad_total,
                       COUNT(DISTINCT fi.factura_id) as veces_usado
                FROM materiales m
                INNER JOIN factura_items fi ON fi.material_id = m.id
                INNER JOIN facturas f ON f.id = fi.factura_id
                """ + whereSql + """
                 GROUP BY m.id, m.nombre HAVING ingresos_total > 0 ORDER BY ingresos_total DESC LIMIT ?
                """;
        params.add(limite);
        List<TopMaterial> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TopMaterial m = new TopMaterial();
                    m.materialNombre = rs.getString("material_nombre");
                    m.ingresosTotal = rs.getDouble("ingresos_total");
                    m.cantidadTotal = rs.getDouble("cantidad_total");
                    m.vecesUsado = rs.getInt("veces_usado");
                    lista.add(m);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error top materiales", e);
        }
        return lista;
    }

    public List<EvolucionMensual> obtenerEvolucionFacturacionMensual(int meses) {
        LocalDate limite = LocalDate.now().minusMonths(meses);
        String fechaLimite = limite.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String sql = """
                SELECT strftime('%Y-%m', f.fecha_creacion) as mes,
                       COALESCE(SUM(CASE WHEN f.estado_pago = 'Pagada' THEN f.total ELSE 0 END), 0) as facturacion_pagada,
                       COALESCE(SUM(CASE WHEN f.estado_pago = 'No Pagada' THEN f.total ELSE 0 END), 0) as facturacion_pendiente
                FROM facturas f
                WHERE DATE(f.fecha_creacion) >= DATE(?)
                GROUP BY strftime('%Y-%m', f.fecha_creacion) ORDER BY mes ASC
                """;
        List<EvolucionMensual> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaLimite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EvolucionMensual e = new EvolucionMensual();
                    e.mes = rs.getString("mes");
                    e.facturacionPagada = rs.getDouble("facturacion_pagada");
                    e.facturacionPendiente = rs.getDouble("facturacion_pendiente");
                    lista.add(e);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error evolución mensual", e);
        }
        return lista;
    }

    public List<String> obtenerMesesDisponiblesPresupuestos() {
        String sql = """
                SELECT DISTINCT strftime('%Y-%m', fecha_creacion) as mes FROM presupuestos
                WHERE fecha_creacion IS NOT NULL ORDER BY mes DESC
                """;
        return obtenerMesesComoLista(sql);
    }

    public List<String> obtenerMesesDisponiblesFacturas() {
        String sql = """
                SELECT DISTINCT strftime('%Y-%m', fecha_creacion) as mes FROM facturas
                WHERE fecha_creacion IS NOT NULL ORDER BY mes DESC
                """;
        return obtenerMesesComoLista(sql);
    }

    private List<String> obtenerMesesComoLista(String sql) {
        List<String> meses = new ArrayList<>();
        String[] nombres = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ym = rs.getString("mes");
                if (ym != null && ym.length() >= 7) {
                    String[] p = ym.split("-");
                    String nombre = p.length == 2 ? nombres[Integer.parseInt(p[1]) - 1] + " " + p[0] + " (" + ym + ")" : ym;
                    meses.add(nombre);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error meses disponibles", e);
        }
        return meses;
    }

    private static void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
