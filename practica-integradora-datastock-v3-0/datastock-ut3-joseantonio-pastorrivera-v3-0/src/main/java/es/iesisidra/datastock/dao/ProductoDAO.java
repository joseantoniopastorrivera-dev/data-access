package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dto.ProductoDetalle;
import es.iesisidra.datastock.jdbc.ConsultaProductoJdbc;
import es.iesisidra.datastock.modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF-JDBC-03 y RF-JDBC-04 · Acceso a la tabla {@code producto}.
 * <p>El stock solo se escribe en el alta. Después, únicamente lo cambia el
 * procedimiento registrar_movimiento (ver {@link MovimientoDAO}); por eso
 * {@link #actualizar} no tiene columna stock.</p>
 */
public class ProductoDAO {
    /** Consulta base con JOIN para obtener también los nombres de categoría y proveedor. */
    private static final String SELECT_DETALLE =
            "SELECT p.codigo, p.nombre, p.precio, p.stock, p.categoria_id, c.nombre AS categoria, "
            + "p.proveedor_id, pr.nombre AS proveedor "
            + "FROM producto p "
            + "JOIN categoria c ON c.id = p.categoria_id "
            + "JOIN proveedor pr ON pr.id = p.proveedor_id ";
    private static final String TODOS = SELECT_DETALLE + "ORDER BY p.codigo";
    private static final String POR_NOMBRE = SELECT_DETALLE + "WHERE p.nombre LIKE ? ORDER BY p.codigo";
    private static final String STOCK_INFERIOR = SELECT_DETALLE + "WHERE p.stock < ? ORDER BY p.stock, p.codigo";
    private static final String EXISTE = "SELECT 1 FROM producto WHERE codigo = ?";
    private static final String INSERTAR =
            "INSERT INTO producto (codigo, nombre, precio, stock, categoria_id, proveedor_id) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR =
            "UPDATE producto SET nombre = ?, precio = ?, categoria_id = ?, proveedor_id = ? WHERE codigo = ?";
    private static final String ELIMINAR = "DELETE FROM producto WHERE codigo = ?";

    /** UT3: la consulta por código está en el componente reutilizable; aquí solo se delega. */
    private final ConsultaProductoJdbc consultaPorCodigo = new ConsultaProductoJdbc();

    /** @return todos los productos ordenados por código */
    public List<ProductoDetalle> listarTodos() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(TODOS);
             ResultSet rs = ps.executeQuery()) {
            return leerTodos(rs);
        }
    }

    /**
     * Lo usan 7.4 (consultar, modificar y eliminar) y 7.6/7.7 (movimientos). Desde la UT3
     * delega en {@link ConsultaProductoJdbc}, para que la SQL no esté repetida.
     * @param codigo código del producto
     */
    public Optional<ProductoDetalle> buscarPorCodigo(String codigo) throws SQLException {
        return consultaPorCodigo.buscarPorCodigo(codigo);
    }

    /**
     * Búsqueda parcial con LIKE. El texto del usuario va como parámetro; los comodines
     * % se añaden en Java, no en la cadena SQL. La colación _ci hace que no distinga
     * mayúsculas ni tildes.
     */
    public List<ProductoDetalle> buscarPorNombre(String fragmento) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(POR_NOMBRE)) {
            ps.setString(1, "%" + escaparLike(fragmento) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return leerTodos(rs);
            }
        }
    }

    /** @return productos con stock estrictamente menor que el umbral, por stock y código */
    public List<ProductoDetalle> stockInferiorA(int umbral) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(STOCK_INFERIOR)) {
            ps.setInt(1, umbral);
            try (ResultSet rs = ps.executeQuery()) {
                return leerTodos(rs);
            }
        }
    }

    public boolean existe(String codigo) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return existe(con, codigo);
        }
    }

    public boolean existe(Connection con, String codigo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(EXISTE)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertar(Producto producto) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(INSERTAR)) {
            rellenarInsercion(ps, producto);
            return ps.executeUpdate();
        }
    }

    /** @return una sentencia de inserción preparada sobre la conexión de una transacción */
    public PreparedStatement prepararInsercion(Connection con) throws SQLException {
        return con.prepareStatement(INSERTAR);
    }

    /** Asigna los 6 parámetros del INSERT. El precio va con setBigDecimal: sin redondeos de double. */
    public void rellenarInsercion(PreparedStatement ps, Producto producto) throws SQLException {
        ps.setString(1, producto.getCodigo());
        ps.setString(2, producto.getNombre());
        ps.setBigDecimal(3, producto.getPrecio());
        ps.setInt(4, producto.getStock());
        ps.setInt(5, producto.getCategoriaId());
        ps.setInt(6, producto.getProveedorId());
    }

    /** Modifica nombre, precio, categoría y proveedor. NO toca el código ni el stock. @return filas afectadas */
    public int actualizar(Producto producto) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(ACTUALIZAR)) {
            ps.setString(1, producto.getNombre());
            ps.setBigDecimal(2, producto.getPrecio());
            ps.setInt(3, producto.getCategoriaId());
            ps.setInt(4, producto.getProveedorId());
            ps.setString(5, producto.getCodigo());
            return ps.executeUpdate();
        }
    }

    /** @throws SQLException 1451 si tiene movimientos (la FK de movimiento_stock lo impide) */
    public int eliminar(String codigo) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(ELIMINAR)) {
            ps.setString(1, codigo);
            return ps.executeUpdate();
        }
    }

    /**
     * En LIKE, % y _ son comodines. Si el usuario busca "50%", se escapan con \
     * para que se busque el carácter literal y no "cualquier cosa".
     */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<ProductoDetalle> leerTodos(ResultSet rs) throws SQLException {
        List<ProductoDetalle> productos = new ArrayList<>();
        while (rs.next()) {
            productos.add(leer(rs));
        }
        return productos;
    }

    /** Transforma la fila actual en un DTO, leyendo cada columna por su nombre. */
    private ProductoDetalle leer(ResultSet rs) throws SQLException {
        return new ProductoDetalle(rs.getString("codigo"), rs.getString("nombre"), rs.getBigDecimal("precio"),
                rs.getInt("stock"), rs.getInt("categoria_id"), rs.getString("categoria"),
                rs.getInt("proveedor_id"), rs.getString("proveedor"));
    }
}
