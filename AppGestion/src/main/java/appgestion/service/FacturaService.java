package appgestion.service;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para gestionar facturas, basado en FacturaManager en Python.
 * Implementa generación de número de factura, creación y listado básico.
 */
public class FacturaService {

    private static final Logger log = Logger.getLogger(FacturaService.class.getName());
    private final double ivaPorcentaje = 21.0;

    public String generarNumeroFactura() throws SQLException {
        String sql = "SELECT numero_factura FROM facturas ORDER BY id DESC LIMIT 1";
        Connection conn = Database.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String ultimo = rs.getString("numero_factura");
                if (ultimo != null && !ultimo.isEmpty()) {
                    String[] partes = ultimo.split("-");
                    if (partes.length == 2) {
                        int numero = Integer.parseInt(partes[0].substring(1));
                        int anio = Integer.parseInt(partes[1]);
                        int anioActual = Year.now().getValue();
                        int nuevoNumero = (anio == anioActual) ? numero + 1 : 1;
                        return String.format("F%04d-%d", nuevoNumero, anioActual);
                    }
                }
            }
        } catch (Exception e) {
            // si hay cualquier error, se ignora para empezar desde F0001
        }
        int anioActual = Year.now().getValue();
        return String.format("F0001-%d", anioActual);
    }

    public int crearFacturaDesdePresupuesto(int presupuestoId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Cargar presupuesto e items
                String presSql = """
                        SELECT p.*, c.nombre as cliente_nombre, c.id as cliente_id
                        FROM presupuestos p
                        JOIN clientes c ON p.cliente_id = c.id
                        WHERE p.id = ?
                        """;
                int clienteId;
                double subtotal;
                double iva;
                double total;
                boolean ivaHabilitado;
                try (PreparedStatement ps = conn.prepareStatement(presSql)) {
                    ps.setInt(1, presupuestoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Presupuesto no encontrado: " + presupuestoId);
                        }
                        clienteId = rs.getInt("cliente_id");
                        subtotal = rs.getDouble("subtotal");
                        iva = rs.getDouble("iva");
                        total = rs.getDouble("total");
                        ivaHabilitado = rs.getInt("iva_habilitado") == 1;
                    }
                }

                String numeroFactura = generarNumeroFactura();

                String insertFactura = """
                        INSERT INTO facturas
                        (numero_factura, cliente_id, presupuesto_id, subtotal, iva, total,
                         iva_habilitado, metodo_pago, estado_pago)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'Transferencia', 'No Pagada')
                        """;
                int facturaId;
                try (PreparedStatement ps = conn.prepareStatement(insertFactura, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, numeroFactura);
                    ps.setInt(2, clienteId);
                    ps.setInt(3, presupuestoId);
                    ps.setDouble(4, subtotal);
                    ps.setDouble(5, iva);
                    ps.setDouble(6, total);
                    ps.setInt(7, ivaHabilitado ? 1 : 0);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("No se pudo obtener el ID de factura creada.");
                        }
                        facturaId = rs.getInt(1);
                    }
                }

                // Copiar items
                String itemsSql = """
                        SELECT *
                        FROM presupuesto_items
                        WHERE presupuesto_id = ?
                        ORDER BY id
                        """;
                String insertItem = """
                        INSERT INTO factura_items
                        (factura_id, material_id, tarea_manual, cantidad,
                         precio_unitario, subtotal, visible_pdf, es_tarea_manual,
                         aplica_iva, descuento_porcentaje, descuento_fijo, cuota_iva)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement psSel = conn.prepareStatement(itemsSql);
                     PreparedStatement psIns = conn.prepareStatement(insertItem)) {
                    psSel.setInt(1, presupuestoId);
                    try (ResultSet rs = psSel.executeQuery()) {
                        while (rs.next()) {
                            double cantidad = rs.getDouble("cantidad");
                            double precioUnitario = rs.getDouble("precio_unitario");
                            double descuentoPorcentaje = rs.getDouble("descuento_porcentaje");
                            double descuentoFijo = rs.getDouble("descuento_fijo");
                            boolean aplicaIva = rs.getInt("aplica_iva") == 1;

                            double itemSubtotalBruto = cantidad * precioUnitario;
                            double itemDescuento = 0.0;
                            if (descuentoPorcentaje > 0) {
                                itemDescuento = itemSubtotalBruto * (descuentoPorcentaje / 100.0);
                            } else if (descuentoFijo > 0) {
                                itemDescuento = Math.min(descuentoFijo, itemSubtotalBruto);
                            }
                            double itemSubtotal = itemSubtotalBruto - itemDescuento;

                            double cuotaIva = 0.0;
                            if (aplicaIva && ivaHabilitado) {
                                cuotaIva = itemSubtotal * (ivaPorcentaje / 100.0);
                            }

                            psIns.setInt(1, facturaId);
                            int materialId = rs.getInt("material_id");
                            if (rs.wasNull()) {
                                psIns.setNull(2, Types.INTEGER);
                            } else {
                                psIns.setInt(2, materialId);
                            }
                            psIns.setString(3, rs.getString("tarea_manual"));
                            psIns.setDouble(4, cantidad);
                            psIns.setDouble(5, precioUnitario);
                            psIns.setDouble(6, itemSubtotal);
                            psIns.setInt(7, rs.getInt("visible_pdf"));
                            psIns.setInt(8, rs.getInt("es_tarea_manual"));
                            psIns.setInt(9, aplicaIva ? 1 : 0);
                            psIns.setDouble(10, descuentoPorcentaje);
                            psIns.setDouble(11, descuentoFijo);
                            psIns.setDouble(12, cuotaIva);
                            psIns.addBatch();
                        }
                    }
                    psIns.executeBatch();
                }

                conn.commit();
                return facturaId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<FacturaResumen> obtenerFacturas() {
        String sql = """
                SELECT f.id, f.numero_factura, f.fecha_creacion, f.subtotal, f.iva, f.total,
                       f.estado_pago, c.nombre as cliente_nombre
                FROM facturas f
                JOIN clientes c ON f.cliente_id = c.id
                ORDER BY f.fecha_creacion DESC
                """;
        List<FacturaResumen> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FacturaResumen r = new FacturaResumen();
                r.id = rs.getInt("id");
                r.numeroFactura = rs.getString("numero_factura");
                r.fechaCreacion = rs.getString("fecha_creacion");
                r.subtotal = rs.getDouble("subtotal");
                r.iva = rs.getDouble("iva");
                r.total = rs.getDouble("total");
                r.estadoPago = rs.getString("estado_pago");
                r.clienteNombre = rs.getString("cliente_nombre");
                lista.add(r);
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return lista;
    }

    public List<Integer> obtenerAniosDisponibles() {
        String sql = """
                SELECT DISTINCT strftime('%Y', fecha_creacion) as anio
                FROM facturas
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
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return anios;
    }

    public List<Integer> obtenerMesesDisponibles(Integer anio) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT strftime('%m', fecha_creacion) as mes
                FROM facturas
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
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return meses;
    }

    public List<FacturaResumen> buscarFacturas(String termino, Integer anio, Integer mes, String estado) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id, f.numero_factura, f.fecha_creacion, f.subtotal, f.iva, f.total,
                       f.estado_pago, c.nombre as cliente_nombre
                FROM facturas f
                JOIN clientes c ON f.cliente_id = c.id
                """);
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (termino != null && !termino.isBlank()) {
            where.add("(c.nombre LIKE ? OR f.numero_factura LIKE ?)");
            String like = "%" + termino + "%";
            params.add(like);
            params.add(like);
        }
        if (anio != null) {
            where.add("strftime('%Y', f.fecha_creacion) = ?");
            params.add(String.valueOf(anio));
        }
        if (mes != null) {
            where.add("strftime('%m', f.fecha_creacion) = ?");
            params.add(String.format("%02d", mes));
        }
        if (estado != null && !estado.isBlank() && !"TODAS".equalsIgnoreCase(estado)) {
            where.add("f.estado_pago = ?");
            params.add(estado);
        }
        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        sql.append(" ORDER BY f.fecha_creacion DESC");

        List<FacturaResumen> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FacturaResumen r = new FacturaResumen();
                    r.id = rs.getInt("id");
                    r.numeroFactura = rs.getString("numero_factura");
                    r.fechaCreacion = rs.getString("fecha_creacion");
                    r.subtotal = rs.getDouble("subtotal");
                    r.iva = rs.getDouble("iva");
                    r.total = rs.getDouble("total");
                    r.estadoPago = rs.getString("estado_pago");
                    r.clienteNombre = rs.getString("cliente_nombre");
                    lista.add(r);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return lista;
    }

    /**
     * Obtiene una factura completa por ID (cabecera + items) para ver detalle y generar PDF.
     */
    public FacturaDetalle obtenerFacturaPorId(int facturaId) {
        String cabSql = """
                SELECT f.id, f.numero_factura, f.fecha_creacion, f.fecha_vencimiento, f.metodo_pago, f.estado_pago, f.notas,
                       f.subtotal, f.iva, f.total, f.iva_habilitado,
                       c.nombre as cliente_nombre, c.telefono, c.email, c.direccion
                FROM facturas f
                JOIN clientes c ON f.cliente_id = c.id
                WHERE f.id = ?
                """;
        String itemsSql = """
                SELECT fi.tarea_manual, fi.cantidad, fi.precio_unitario, fi.subtotal, fi.es_tarea_manual,
                       m.nombre as material_nombre, m.unidad_medida
                FROM factura_items fi
                LEFT JOIN materiales m ON fi.material_id = m.id
                WHERE fi.factura_id = ?
                ORDER BY fi.id
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement psCab = conn.prepareStatement(cabSql);
             PreparedStatement psItems = conn.prepareStatement(itemsSql)) {
            psCab.setInt(1, facturaId);
            try (ResultSet rsCab = psCab.executeQuery()) {
                if (!rsCab.next()) return null;
                FacturaDetalle d = new FacturaDetalle();
                d.id = rsCab.getInt("id");
                d.numeroFactura = rsCab.getString("numero_factura");
                d.fechaCreacion = rsCab.getString("fecha_creacion");
                d.fechaVencimiento = rsCab.getString("fecha_vencimiento");
                d.metodoPago = rsCab.getString("metodo_pago");
                d.estadoPago = rsCab.getString("estado_pago");
                d.notas = rsCab.getString("notas");
                d.clienteNombre = rsCab.getString("cliente_nombre");
                d.telefono = rsCab.getString("telefono");
                d.email = rsCab.getString("email");
                d.direccion = rsCab.getString("direccion");
                d.subtotal = rsCab.getDouble("subtotal");
                d.iva = rsCab.getDouble("iva");
                d.total = rsCab.getDouble("total");
                d.ivaHabilitado = rsCab.getInt("iva_habilitado") == 1;
                d.items = new ArrayList<>();
                psItems.setInt(1, facturaId);
                try (ResultSet rsItems = psItems.executeQuery()) {
                    while (rsItems.next()) {
                        FacturaItemDetalle it = new FacturaItemDetalle();
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
            log.log(Level.SEVERE, "Error en operación", e);
            return null;
        }
    }

    public static class FacturaDetalle {
        public int id;
        public String numeroFactura;
        public String fechaCreacion;
        public String fechaVencimiento;
        public String metodoPago;
        public String estadoPago;
        public String notas;
        public String clienteNombre;
        public String telefono;
        public String email;
        public String direccion;
        public double subtotal;
        public double iva;
        public double total;
        public boolean ivaHabilitado;
        public List<FacturaItemDetalle> items;
    }

    public static class FacturaItemDetalle {
        public boolean esTareaManual;
        public String tareaManual;
        public String materialNombre;
        public String unidadMedida;
        public double cantidad;
        public double precioUnitario;
        public double subtotal;
    }

    public static class FacturaResumen {
        public int id;
        public String numeroFactura;
        public String fechaCreacion;
        public double subtotal;
        public double iva;
        public double total;
        public String estadoPago;
        public String clienteNombre;

        public int getId() { return id; }
        public String getNumeroFactura() { return numeroFactura; }
        public String getFechaCreacion() { return fechaCreacion; }
        public double getSubtotal() { return subtotal; }
        public double getIva() { return iva; }
        public double getTotal() { return total; }
        public String getEstadoPago() { return estadoPago; }
        public String getClienteNombre() { return clienteNombre; }
    }
}

