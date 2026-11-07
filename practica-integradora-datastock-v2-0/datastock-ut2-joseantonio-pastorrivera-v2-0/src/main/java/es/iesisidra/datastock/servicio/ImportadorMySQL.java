package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.dao.ProveedorDAO;
import es.iesisidra.datastock.jdbc.ErroresBD;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.validacion.ValidadorProducto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RF-JDBC-07 · Copia a MySQL los productos del inventario en memoria (UT1).
 * <p>Funciona en dos fases:</p>
 * <ol>
 *   <li><b>Validación</b>: cada producto se comprueba (reglas de campo, código
 *   duplicado en MySQL, existencia de categoría y proveedor). Los que fallan se
 *   omiten o rechazan con su motivo y no llegan a enviarse a MySQL.</li>
 *   <li><b>Inserción</b>: los aceptados se insertan en UNA transacción JDBC:
 *   {@code setAutoCommit(false)}, un INSERT por producto y {@code commit()} al
 *   final. Si salta una SQLException inesperada, {@code rollback()} deshace
 *   todo el lote: nunca queda insertada solo una parte.</li>
 * </ol>
 * <p>La lista recibida no se modifica: memoria y MySQL siguen siendo almacenes separados.</p>
 */
public class ImportadorMySQL {
    /**
     * Propiedad SOLO para la prueba CP-JDBC-18: con -Ddatastock.simularFalloLote=true,
     * tras insertar el lote se repite el primer INSERT, lo que provoca un error real
     * de clave duplicada (1062) dentro de la transacción y obliga a hacer rollback.
     * Por defecto está desactivada.
     */
    public static final String PROPIEDAD_FALLO_SIMULADO = "datastock.simularFalloLote";

    /** Qué le ha pasado a cada producto analizado. */
    public enum Estado { VALIDO, DUPLICADO, RECHAZADO }

    /** Resultado del análisis de un producto, para que la consola lo muestre. */
    public record Linea(String codigo, String nombre, Estado estado, String motivo) { }

    /**
     * Resumen de la importación.
     * @param insertados productos realmente confirmados con commit (0 si hubo rollback)
     * @param errorLote  explicación del error SQL si hubo rollback; null si no lo hubo
     */
    public record Resultado(List<Linea> lineas, int insertados, int omitidos, int rechazados, String errorLote) {
        public boolean huboRollback() { return errorLote != null; }
    }

    private final ValidadorProducto validador = new ValidadorProducto();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    /**
     * @param productos productos en memoria (no se modifican)
     * @throws SQLException solo si no se puede ni conectar o fallan las consultas de validación
     */
    public Resultado importar(List<Producto> productos) throws SQLException {
        List<Linea> lineas = new ArrayList<>();
        List<Producto> aceptados = new ArrayList<>();
        int omitidos = 0;
        int rechazados = 0;

        try (Connection con = ConexionBD.obtenerConexion()) {
            // ---- Fase 1: validación, sin escribir nada ----
            Set<String> codigosDelLote = new HashSet<>();
            for (Producto original : productos) {
                List<String> errores = new ArrayList<>();
                Producto producto = validador.validar(original, errores);
                String nombre = original.getNombre();
                if (producto == null) {
                    lineas.add(new Linea(original.getCodigo(), nombre, Estado.RECHAZADO, String.join("; ", errores)));
                    rechazados++;
                } else if (productoDAO.existe(con, producto.getCodigo()) || !codigosDelLote.add(producto.getCodigo())) {
                    lineas.add(new Linea(producto.getCodigo(), nombre, Estado.DUPLICADO,
                            "El producto ya existe en MySQL. Se omite."));
                    omitidos++;
                } else {
                    String motivo = comprobarReferencias(con, producto);
                    if (motivo != null) {
                        lineas.add(new Linea(producto.getCodigo(), nombre, Estado.RECHAZADO, motivo));
                        rechazados++;
                        codigosDelLote.remove(producto.getCodigo());
                    } else {
                        lineas.add(new Linea(producto.getCodigo(), nombre, Estado.VALIDO, "Producto válido para importar."));
                        aceptados.add(producto);
                    }
                }
            }
            if (aceptados.isEmpty()) {
                return new Resultado(lineas, 0, omitidos, rechazados, null);
            }

            // ---- Fase 2: inserción del lote en una única transacción ----
            boolean autoCommitPrevio = con.getAutoCommit();
            con.setAutoCommit(false);
            try (PreparedStatement ps = productoDAO.prepararInsercion(con)) {
                for (Producto producto : aceptados) {
                    productoDAO.rellenarInsercion(ps, producto);
                    ps.executeUpdate();
                }
                if (Boolean.getBoolean(PROPIEDAD_FALLO_SIMULADO)) {
                    productoDAO.rellenarInsercion(ps, aceptados.get(0));
                    ps.executeUpdate(); // repite el primer código: error 1062 dentro de la transacción
                }
                con.commit();
                return new Resultado(lineas, aceptados.size(), omitidos, rechazados, null);
            } catch (SQLException ex) {
                con.rollback(); // deshace TODOS los INSERT de este lote
                return new Resultado(lineas, 0, omitidos, rechazados, ErroresBD.explicar(ex));
            } finally {
                con.setAutoCommit(autoCommitPrevio);
            }
        }
    }

    /** @return el motivo del rechazo, o null si la categoría y el proveedor existen */
    private String comprobarReferencias(Connection con, Producto producto) throws SQLException {
        if (!categoriaDAO.existe(con, producto.getCategoriaId())) {
            return "No existe la categoría con ID " + producto.getCategoriaId() + ". El producto no se puede importar.";
        }
        if (!proveedorDAO.existe(con, producto.getProveedorId())) {
            return "No existe el proveedor con ID " + producto.getProveedorId() + ". El producto no se puede importar.";
        }
        return null;
    }
}
