package appgestion.service;

import appgestion.model.PresupuestoItemRow;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para gestionar presupuestos.
 * Porta la lógica de PresupuestoManager en Python (creación y cálculo de totales).
 */
public class PresupuestoService {

    private static final Logger log = Logger.getLogger(PresupuestoService.class.getName());
    private final double ivaPorcentaje = 21.0; // 21% de IVA

    public int crearPresupuesto(
            int clienteId,
            List<PresupuestoItemRow> items,
            boolean ivaHabilitado,
            double descuentoGlobalPorcentaje,
            double descuentoGlobalFijo,
            boolean descuentoAntesIva
    ) throws SQLException {
        TotalesPresupuesto totales = calcularTotalesCompleto(
                items,
                descuentoGlobalPorcentaje,
                descuentoGlobalFijo,
                descuentoAntesIva,
                ivaHabilitado
        );

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String insertPresupuesto = """
                        INSERT INTO presupuestos (cliente_id, subtotal, iva, total, iva_habilitado,
                                                  descuento_global_porcentaje, descuento_global_fijo, descuento_antes_iva)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                int presupuestoId;
                try (PreparedStatement ps = conn.prepareStatement(insertPresupuesto, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, clienteId);
                    ps.setDouble(2, totales.subtotal);
                    ps.setDouble(3, totales.iva);
                    ps.setDouble(4, totales.total);
                    ps.setInt(5, ivaHabilitado ? 1 : 0);
                    ps.setDouble(6, descuentoGlobalPorcentaje);
                    ps.setDouble(7, descuentoGlobalFijo);
                    ps.setInt(8, descuentoAntesIva ? 1 : 0);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("No se pudo obtener el ID del presupuesto creado.");
                        }
                        presupuestoId = rs.getInt(1);
                    }
                }

                String insertItem = """
                        INSERT INTO presupuesto_items
                        (presupuesto_id, material_id, tarea_manual, cantidad, precio_unitario, subtotal,
                         visible_pdf, es_tarea_manual, aplica_iva, descuento_porcentaje, descuento_fijo)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement psItem = conn.prepareStatement(insertItem)) {
                    for (PresupuestoItemRow row : items) {
                        double itemSubtotal = row.getCantidad() * row.getPrecioUnitario();
                        psItem.setInt(1, presupuestoId);
                        if (row.isEsTareaManual() || row.getMaterialId() == 0) {
                            psItem.setNull(2, Types.INTEGER);
                        } else {
                            psItem.setInt(2, row.getMaterialId());
                        }
                        psItem.setString(3, row.getDescripcion());
                        psItem.setDouble(4, row.getCantidad());
                        psItem.setDouble(5, row.getPrecioUnitario());
                        psItem.setDouble(6, itemSubtotal);
                        psItem.setInt(7, row.isVisiblePdf() ? 1 : 0);
                        psItem.setInt(8, row.isEsTareaManual() ? 1 : 0);
                        psItem.setInt(9, row.isAplicaIva() ? 1 : 0);
                        psItem.setDouble(10, row.getDescuentoPorcentaje());
                        psItem.setDouble(11, row.getDescuentoFijo());
                        psItem.addBatch();
                    }
                    psItem.executeBatch();
                }

                conn.commit();
                return presupuestoId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public TotalesPresupuesto calcularTotalesCompleto(
            List<PresupuestoItemRow> items,
            double descuentoGlobalPorcentaje,
            double descuentoGlobalFijo,
            boolean descuentoAntesIva,
            boolean ivaHabilitado
    ) {
        double subtotal = 0.0;
        double descuentosItems = 0.0;
        double ivaBase = 0.0;

        boolean hayItemsConIva = items.stream().anyMatch(i -> i.isAplicaIva());
        if (!hayItemsConIva && !items.isEmpty()) {
            ivaHabilitado = false;
        }

        for (PresupuestoItemRow item : items) {
            double itemSubtotal = item.getCantidad() * item.getPrecioUnitario();

            double itemDescuento = 0.0;
            if (item.getDescuentoPorcentaje() > 0) {
                itemDescuento = itemSubtotal * (item.getDescuentoPorcentaje() / 100.0);
            } else if (item.getDescuentoFijo() > 0) {
                itemDescuento = Math.min(item.getDescuentoFijo(), itemSubtotal);
            }

            double baseLinea = itemSubtotal - itemDescuento;
            subtotal += baseLinea;
            descuentosItems += itemDescuento;

            if (item.isAplicaIva() && ivaHabilitado) {
                ivaBase += baseLinea * (ivaPorcentaje / 100.0);
            }

            item.setSubtotal(baseLinea);
        }

        double iva = ivaBase;
        double descuentoGlobal = 0.0;
        double total;

        if (descuentoAntesIva) {
            if (descuentoGlobalPorcentaje > 0) {
                descuentoGlobal = subtotal * (descuentoGlobalPorcentaje / 100.0);
            } else if (descuentoGlobalFijo > 0) {
                descuentoGlobal = Math.min(descuentoGlobalFijo, subtotal);
            }

            double baseIva = subtotal - descuentoGlobal;
            if (subtotal > 0) {
                iva = ivaBase * (baseIva / subtotal);
            } else {
                iva = 0.0;
            }
            total = baseIva + iva;
        } else {
            double totalSinDescuento = subtotal + iva;
            if (descuentoGlobalPorcentaje > 0) {
                descuentoGlobal = totalSinDescuento * (descuentoGlobalPorcentaje / 100.0);
            } else if (descuentoGlobalFijo > 0) {
                descuentoGlobal = Math.min(descuentoGlobalFijo, totalSinDescuento);
            }
            total = totalSinDescuento - descuentoGlobal;
        }

        TotalesPresupuesto t = new TotalesPresupuesto();
        t.subtotal = subtotal;
        t.descuentosItems = descuentosItems;
        t.descuentoGlobal = descuentoGlobal;
        t.iva = iva;
        t.total = total;
        t.ivaPorcentaje = ivaPorcentaje;
        t.descuentoAntesIva = descuentoAntesIva;
        return t;
    }

    public List<PresupuestoResumen> obtenerPresupuestos() {
        String sql = """
                SELECT p.id, p.fecha_creacion, p.subtotal, p.iva, p.total,
                       c.nombre as cliente_nombre
                FROM presupuestos p
                JOIN clientes c ON p.cliente_id = c.id
                ORDER BY p.fecha_creacion DESC
                """;
        List<PresupuestoResumen> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PresupuestoResumen r = new PresupuestoResumen();
                r.id = rs.getInt("id");
                r.fechaCreacion = rs.getString("fecha_creacion");
                r.subtotal = rs.getDouble("subtotal");
                r.iva = rs.getDouble("iva");
                r.total = rs.getDouble("total");
                r.clienteNombre = rs.getString("cliente_nombre");
                lista.add(r);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en obtenerPresupuestos", e);
        }
        return lista;
    }

    public List<Integer> obtenerAniosDisponibles() {
        String sql = """
                SELECT DISTINCT strftime('%Y', fecha_creacion) as anio
                FROM presupuestos
                WHERE fecha_creacion IS NOT NULL
                ORDER BY anio DESC
                """;
        List<Integer> anios = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String anioStr = rs.getString("anio");
                if (anioStr != null) {
                    anios.add(Integer.valueOf(anioStr));
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en obtenerAniosDisponibles", e);
        }
        return anios;
    }

    public List<Integer> obtenerMesesDisponibles(Integer anio) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT strftime('%m', fecha_creacion) as mes
                FROM presupuestos
                WHERE fecha_creacion IS NOT NULL
                """);
        List<Integer> meses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (anio != null) {
            sql.append(" AND strftime('%Y', fecha_creacion) = ?");
            params.add(String.valueOf(anio));
        }
        sql.append(" ORDER BY mes DESC");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mesStr = rs.getString("mes");
                    if (mesStr != null) {
                        meses.add(Integer.valueOf(mesStr));
                    }
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en obtenerMesesDisponibles", e);
        }
        return meses;
    }

    public List<PresupuestoResumen> buscarPresupuestos(String termino, Integer anio, Integer mes) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.fecha_creacion, p.subtotal, p.iva, p.total,
                       c.nombre as cliente_nombre
                FROM presupuestos p
                JOIN clientes c ON p.cliente_id = c.id
                """);
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (termino != null && !termino.isBlank()) {
            where.add("(c.nombre LIKE ? OR CAST(p.id AS TEXT) LIKE ? OR p.fecha_creacion LIKE ?)");
            String like = "%" + termino + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (anio != null) {
            where.add("strftime('%Y', p.fecha_creacion) = ?");
            params.add(String.valueOf(anio));
        }
        if (mes != null) {
            where.add("strftime('%m', p.fecha_creacion) = ?");
            params.add(String.format("%02d", mes));
        }
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        sql.append(" ORDER BY p.fecha_creacion DESC");

        List<PresupuestoResumen> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PresupuestoResumen r = new PresupuestoResumen();
                    r.id = rs.getInt("id");
                    r.fechaCreacion = rs.getString("fecha_creacion");
                    r.subtotal = rs.getDouble("subtotal");
                    r.iva = rs.getDouble("iva");
                    r.total = rs.getDouble("total");
                    r.clienteNombre = rs.getString("cliente_nombre");
                    lista.add(r);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en buscarPresupuestos", e);
        }
        return lista;
    }

    /**
     * Obtiene un presupuesto completo por ID (cabecera + items) para ver detalle y generar PDF.
     */
    public PresupuestoDetalle obtenerPresupuestoPorId(int presupuestoId) {
        String cabSql = """
                SELECT p.id, p.cliente_id, p.fecha_creacion, p.subtotal, p.iva, p.total, p.iva_habilitado,
                       c.nombre as cliente_nombre, c.telefono, c.email, c.direccion
                FROM presupuestos p
                JOIN clientes c ON p.cliente_id = c.id
                WHERE p.id = ?
                """;
        String itemsSql = """
                SELECT pi.material_id, pi.tarea_manual, pi.cantidad, pi.precio_unitario, pi.subtotal, pi.es_tarea_manual,
                       m.nombre as material_nombre, m.unidad_medida
                FROM presupuesto_items pi
                LEFT JOIN materiales m ON pi.material_id = m.id
                WHERE pi.presupuesto_id = ?
                ORDER BY pi.id
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement psCab = conn.prepareStatement(cabSql);
             PreparedStatement psItems = conn.prepareStatement(itemsSql)) {
            psCab.setInt(1, presupuestoId);
            try (ResultSet rsCab = psCab.executeQuery()) {
                if (!rsCab.next()) return null;
                PresupuestoDetalle d = new PresupuestoDetalle();
                d.id = rsCab.getInt("id");
                d.clienteId = rsCab.getInt("cliente_id");
                d.fechaCreacion = rsCab.getString("fecha_creacion");
                d.clienteNombre = rsCab.getString("cliente_nombre");
                d.telefono = rsCab.getString("telefono");
                d.email = rsCab.getString("email");
                d.direccion = rsCab.getString("direccion");
                d.subtotal = rsCab.getDouble("subtotal");
                d.iva = rsCab.getDouble("iva");
                d.total = rsCab.getDouble("total");
                d.ivaHabilitado = rsCab.getInt("iva_habilitado") == 1;
                d.items = new ArrayList<>();
                psItems.setInt(1, presupuestoId);
                try (ResultSet rsItems = psItems.executeQuery()) {
                    while (rsItems.next()) {
                        PresupuestoItemDetalle it = new PresupuestoItemDetalle();
                        int mid = rsItems.getInt("material_id");
                        it.materialId = rsItems.wasNull() ? null : mid;
                        it.esTareaManual = rsItems.getInt("es_tarea_manual") == 1;
                        it.tareaManual = rsItems.getString("tarea_manual");
                        it.materialNombre = rsItems.getString("material_nombre");
                        it.unidadMedida = rsItems.getString("unidad_medida");
                        it.cantidad = rsItems.getDouble("cantidad");
                        it.precioUnitario = rsItems.getDouble("precio_unitario");
                        it.subtotal = rsItems.getDouble("subtotal");
                        d.items.add(it);
                    }
                }
                return d;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en obtenerPresupuestoPorId", e);
            return null;
        }
    }

    public static class PresupuestoDetalle {
        public int id;
        public int clienteId;
        public String fechaCreacion;
        public String clienteNombre;
        public String telefono;
        public String email;
        public String direccion;
        public double subtotal;
        public double iva;
        public double total;
        public boolean ivaHabilitado;
        public List<PresupuestoItemDetalle> items;
    }

    public static class PresupuestoItemDetalle {
        public Integer materialId;
        public boolean esTareaManual;
        public String tareaManual;
        public String materialNombre;
        public String unidadMedida;
        public double cantidad;
        public double precioUnitario;
        public double subtotal;
    }

    public static class TotalesPresupuesto {
        public double subtotal;
        public double descuentosItems;
        public double descuentoGlobal;
        public double iva;
        public double total;
        public double ivaPorcentaje;
        public boolean descuentoAntesIva;
    }

    public static class PresupuestoResumen {
        public int id;
        public String fechaCreacion;
        public double subtotal;
        public double iva;
        public double total;
        public String clienteNombre;

        public int getId() { return id; }
        public String getFechaCreacion() { return fechaCreacion; }
        public double getSubtotal() { return subtotal; }
        public double getIva() { return iva; }
        public double getTotal() { return total; }
        public String getClienteNombre() { return clienteNombre; }
    }
}

