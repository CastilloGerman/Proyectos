package appgestion.service;

import appgestion.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de acceso a datos para clientes.
 * Equivalente aproximado a ClienteManager en Python.
 */
public class ClienteService {

    private static final Logger log = Logger.getLogger(ClienteService.class.getName());

    public List<Cliente> obtenerTodos() {
        String sql = "SELECT * FROM clientes ORDER BY nombre";
        List<Cliente> lista = new ArrayList<>();
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

    public List<Cliente> buscar(String termino) {
        String like = "%" + termino + "%";
        String sql = """
                SELECT * FROM clientes
                WHERE nombre LIKE ? OR telefono LIKE ? OR email LIKE ? OR dni LIKE ?
                ORDER BY nombre
                """;
        List<Cliente> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
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

    public Cliente crear(String nombre, String telefono, String email, String direccion, String dni) {
        String sql = """
                INSERT INTO clientes (nombre, telefono, email, direccion, dni)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, telefono);
            ps.setString(3, email);
            ps.setString(4, direccion);
            ps.setString(5, dni);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                return null;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new Cliente(id, nombre, telefono, email, direccion, dni);
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
        }
        return null;
    }

    public boolean actualizar(Cliente cliente) {
        String sql = """
                UPDATE clientes
                SET nombre = ?, telefono = ?, email = ?, direccion = ?, dni = ?
                WHERE id = ?
                """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getTelefono());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getDireccion());
            ps.setString(5, cliente.getDni());
            ps.setInt(6, cliente.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
            return false;
        }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Error en operación", e);
            return false;
        }
    }

    private Cliente fromRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nombre = rs.getString("nombre");
        String telefono = rs.getString("telefono");
        String email = rs.getString("email");
        String direccion = rs.getString("direccion");
        String dni = rs.getString("dni");
        return new Cliente(id, nombre != null ? nombre : "", telefono != null ? telefono : "",
                email != null ? email : "", direccion != null ? direccion : "", dni != null ? dni : "");
    }
}

