package es.iesisidra.datastock.jdbc;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dto.ProductoDetalle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/**
 * UT3 · Componente reutilizable que consulta un producto de MySQL por su código mediante JDBC.
 *
 * <p>Recibe un código, abre una conexión con la configuración JDBC de DataStock
 * ({@link ConexionBD}), ejecuta una consulta con {@link PreparedStatement}, recorre el
 * {@link ResultSet} y devuelve el producto como un {@link ProductoDetalle}: los datos de
 * la tabla {@code producto} más los nombres de su categoría y su proveedor (JOIN).</p>
 *
 * <p>Si no existe ningún producto con ese código devuelve {@link Optional#empty()}: así
 * quien lo llama sabe con claridad que no existe, sin usar {@code null} ni excepciones.</p>
 *
 * <p>El componente no muestra mensajes ni lee del teclado. Los errores de base de datos
 * (MySQL parado, credenciales incorrectas, falta de configuración...) se propagan como
 * {@link SQLException} para que decida quien lo usa: el menú los traduce con
 * {@link ErroresBD}; una prueba JUnit puede comprobarlos.</p>
 *
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * ConsultaProductoJdbc consulta = new ConsultaProductoJdbc();
 * Optional<ProductoDetalle> producto = consulta.buscarPorCodigo("TEC-001");
 * producto.ifPresentOrElse(p -> ..., () -> ...);
 * }</pre>
 *
 * <p>La consulta procede de {@code ProductoDAO.buscarPorCodigo} (UT2, RF-JDBC-04), que
 * ahora delega en este componente para que la sentencia SQL esté en un único sitio.</p>
 */
public class ConsultaProductoJdbc {
    /** Consulta parametrizada: el código va en el "?", nunca concatenado (evita la inyección SQL). */
    private static final String POR_CODIGO =
            "SELECT p.codigo, p.nombre, p.precio, p.stock, p.categoria_id, c.nombre AS categoria, "
            + "p.proveedor_id, pr.nombre AS proveedor "
            + "FROM producto p "
            + "JOIN categoria c ON c.id = p.categoria_id "
            + "JOIN proveedor pr ON pr.id = p.proveedor_id "
            + "WHERE p.codigo = ?";

    /**
     * Busca en MySQL el producto con el código indicado.
     *
     * <p>El código se normaliza antes de consultar (sin espacios extremos y en mayúsculas),
     * porque en la base de datos los códigos se guardan así. La conexión, la sentencia y el
     * resultado se cierran siempre al terminar, también si hay un error, gracias a
     * try-with-resources.</p>
     *
     * @param codigo código del producto, por ejemplo {@code "TEC-001"}
     * @return el producto con los nombres de su categoría y proveedor, o
     *         {@link Optional#empty()} si no existe ningún producto con ese código
     * @throws IllegalArgumentException si el código es nulo o está en blanco
     * @throws SQLException si falta la configuración JDBC, no se puede conectar con MySQL
     *         o la consulta falla
     */
    public Optional<ProductoDetalle> buscarPorCodigo(String codigo) throws SQLException {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del producto es obligatorio.");
        }
        String normalizado = codigo.trim().toUpperCase(Locale.ROOT);

        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(POR_CODIGO)) {
            ps.setString(1, normalizado);
            try (ResultSet rs = ps.executeQuery()) {
                // El código es la clave primaria: como mucho hay una fila.
                return rs.next() ? Optional.of(leer(rs)) : Optional.empty();
            }
        }
    }

    /** Transforma la fila actual del ResultSet en un objeto, leyendo cada columna por su nombre. */
    private ProductoDetalle leer(ResultSet rs) throws SQLException {
        return new ProductoDetalle(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"),
                rs.getInt("stock"), rs.getInt("categoria_id"), rs.getString("categoria"),
                rs.getInt("proveedor_id"), rs.getString("proveedor"));
    }
}
