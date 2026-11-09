package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Categoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF-JDBC-02 · Acceso a la tabla {@code categoria}.
 * <p>Solo contiene SQL: no lee de teclado ni imprime. Todos los valores que vienen
 * del usuario se pasan con PreparedStatement (parámetros {@code ?}), nunca
 * concatenados, para evitar inyección SQL y problemas con comillas.</p>
 * <p>Los métodos que reciben una {@link Connection} la usan sin cerrarla: sirven
 * para participar en una transacción que controla otra clase (RF-JDBC-08).</p>
 */
public class CategoriaDAO {
    private static final String INSERTAR = "INSERT INTO categoria (id, nombre) VALUES (?, ?)";
    private static final String LISTAR = "SELECT id, nombre FROM categoria ORDER BY id";
    private static final String POR_ID = "SELECT id, nombre FROM categoria WHERE id = ?";
    private static final String NOMBRE_EN_USO = "SELECT 1 FROM categoria WHERE nombre = ? AND id <> ?";
    private static final String ACTUALIZAR = "UPDATE categoria SET nombre = ? WHERE id = ?";
    private static final String ELIMINAR = "DELETE FROM categoria WHERE id = ?";

    /** @return filas insertadas (1) */
    public int insertar(Categoria categoria) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return insertar(con, categoria);
        }
    }

    /** Versión para usar dentro de una transacción: no cierra la conexión. */
    public int insertar(Connection con, Categoria categoria) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERTAR)) {
            ps.setInt(1, categoria.getId());
            ps.setString(2, categoria.getNombre());
            return ps.executeUpdate();
        }
    }

    /** @return todas las categorías ordenadas por ID (lista vacía si no hay ninguna) */
    public List<Categoria> listar() throws SQLException {
        List<Categoria> categorias = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categorias.add(leer(rs));
            }
        }
        return categorias;
    }

    public Optional<Categoria> buscarPorId(int id) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return buscarPorId(con, id);
        }
    }

    public Optional<Categoria> buscarPorId(Connection con, int id) throws SQLException {
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

    /**
     * Comprueba si OTRA categoría ya usa ese nombre.
     * <p>La comparación la hace MySQL con la colación utf8mb4_unicode_ci, igual que
     * su índice UNIQUE: "periféricos", "PERIFÉRICOS" y "Perifericos" cuentan como iguales.</p>
     * @param idExcluido ID de la propia categoría al modificarla (0 en un alta)
     */
    public boolean nombreEnUso(String nombre, int idExcluido) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(NOMBRE_EN_USO)) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** @return filas modificadas: 1 si existía, 0 si no existe esa categoría */
    public int actualizarNombre(int id, String nombre) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(ACTUALIZAR)) {
            ps.setString(1, nombre);
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    /**
     * @return filas borradas: 1 o 0 si no existía
     * @throws SQLException con código 1451 si tiene productos asociados (lo impide la FK)
     */
    public int eliminar(int id) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(ELIMINAR)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /** Convierte la fila actual del ResultSet en un objeto Categoria. */
    private Categoria leer(ResultSet rs) throws SQLException {
        return new Categoria(rs.getInt("id"), rs.getString("nombre"));
    }
}
