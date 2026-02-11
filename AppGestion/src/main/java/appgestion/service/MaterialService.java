package appgestion.service;

import appgestion.model.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de acceso a datos para materiales.
 * Equivalente aproximado a MaterialManager en Python.
 */
public class MaterialService {

    private static final Logger log = Logger.getLogger(MaterialService.class.getName());

    public List<Material> obtenerTodos() {
        String sql = "SELECT * FROM materiales ORDER BY nombre";
        List<Material> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(fromRow(rs));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return lista;
    }

    public List<Material> buscar(String termino) {
        String like = "%" + termino + "%";
        String sql = """
                SELECT * FROM materiales
                WHERE nombre LIKE ?
                ORDER BY nombre
                """;
        List<Material> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(fromRow(rs));
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return lista;
    }

    public Material crear(String nombre, String unidadMedida, double precioUnitario) {
        String sql = """
                INSERT INTO materiales (nombre, unidad_medida, precio_unitario)
                VALUES (?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, unidadMedida);
            ps.setDouble(3, precioUnitario);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Material(id, nombre, unidadMedida, precioUnitario);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return null;
    }

    public boolean actualizar(Material material) {
        String sql = """
                UPDATE materiales
                SET nombre = ?, unidad_medida = ?, precio_unitario = ?
                WHERE id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, material.getNombre());
            ps.setString(2, material.getUnidadMedida());
            ps.setDouble(3, material.getPrecioUnitario());
            ps.setInt(4, material.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM materiales WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
            return false;
        }
    }

    private Material fromRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre");
        String unidad = rs.getString("unidad_medida");
        double precio = rs.getDouble("precio_unitario");
        return new Material(
                id,
                nombre != null ? nombre : "",
                unidad != null ? unidad : "",
                precio
        );
    }
}

