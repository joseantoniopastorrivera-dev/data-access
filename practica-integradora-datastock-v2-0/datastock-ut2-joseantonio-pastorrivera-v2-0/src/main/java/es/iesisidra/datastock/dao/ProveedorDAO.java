package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Proveedor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF-JDBC-02 · Acceso a la tabla {@code proveedor}. El email es opcional:
 * cuando no se indica se guarda NULL (no una cadena vacía), porque la columna es
 * UNIQUE y MySQL permite varios NULL, pero no dos cadenas vacías.
 */
public class ProveedorDAO {
    private static final String INSERTAR = "INSERT INTO proveedor (id, nombre, email) VALUES (?, ?, ?)";
    private static final String LISTAR = "SELECT id, nombre, email FROM proveedor ORDER BY id";
    private static final String POR_ID = "SELECT id, nombre, email FROM proveedor WHERE id = ?";
    private static final String EMAIL_EN_USO = "SELECT 1 FROM proveedor WHERE email = ? AND id <> ?";
    private static final String ACTUALIZAR = "UPDATE proveedor SET nombre = ?, email = ? WHERE id = ?";
    private static final String ELIMINAR = "DELETE FROM proveedor WHERE id = ?";

    public int insertar(Proveedor proveedor) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(INSERTAR)) {
            ps.setInt(1, proveedor.getId());
            ps.setString(2, proveedor.getNombre());
            ponerEmail(ps, 3, proveedor.getEmail());
            return ps.executeUpdate();
        }
    }

    public List<Proveedor> listar() throws SQLException {
        List<Proveedor> proveedores = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                proveedores.add(leer(rs));
            }
        }
        return proveedores;
    }

    public Optional<Proveedor> buscarPorId(int id) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return buscarPorId(con, id);
        }
    }

    public Optional<Proveedor> buscarPorId(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(leer(rs)) : Optional.empty();
            }
        }
    }

    public boolean existe(Connection con, int id) throws SQLException {
        return buscarPorId(con, id).isPresent();
    }

    /** @param idExcluido el propio proveedor al modificarlo (0 en un alta) */
    public boolean emailEnUso(String email, int idExcluido) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(EMAIL_EN_USO)) {
            ps.setString(1, email);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** @return 1 si se modificó, 0 si no existe ese ID */
    public int actualizar(Proveedor proveedor) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(ACTUALIZAR)) {
            ps.setString(1, proveedor.getNombre());
            ponerEmail(ps, 2, proveedor.getEmail());
            ps.setInt(3, proveedor.getId());
            return ps.executeUpdate();
        }
    }

    /** @throws SQLException 1451 si hay productos que lo referencian */
    public int eliminar(int id) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(ELIMINAR)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /** setNull con el tipo SQL indica a MySQL "sin valor"; setString("") guardaría un texto vacío. */
    private void ponerEmail(PreparedStatement ps, int posicion, String email) throws SQLException {
        if (email == null) {
            ps.setNull(posicion, Types.VARCHAR);
        } else {
            ps.setString(posicion, email);
        }
    }

    private Proveedor leer(ResultSet rs) throws SQLException {
        // getString devuelve null si la columna es NULL.
        return new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("email"));
    }
}
