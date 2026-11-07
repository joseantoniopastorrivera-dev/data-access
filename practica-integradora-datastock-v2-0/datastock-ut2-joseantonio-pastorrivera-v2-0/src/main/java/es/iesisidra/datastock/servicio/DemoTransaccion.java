package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.jdbc.ErroresBD;
import es.iesisidra.datastock.modelo.Categoria;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-JDBC-08 · Demostración técnica de commit y rollback controlados desde Java.
 * <p>Trabaja solo con categorías de prueba a partir del ID {@value #PRIMER_ID_PRUEBA},
 * un rango que no usan los datos reales, y siempre comprueba antes que los IDs
 * están libres. No se confunde con el rollback del procedimiento almacenado:
 * aquí la transacción la abre y la cierra Java.</p>
 * <p>Cada escenario devuelve los pasos realizados como texto para que la consola
 * los muestre; esta clase no imprime nada.</p>
 */
public class DemoTransaccion {
    /** Inicio del rango de IDs reservado para pruebas. */
    public static final int PRIMER_ID_PRUEBA = 9001;
    private static final String PREFIJO_NOMBRE = "DEMO-TX ";
    private static final String BORRAR_DEMO =
            "DELETE FROM categoria WHERE id = ? AND nombre LIKE 'DEMO-TX %' "
            + "AND NOT EXISTS (SELECT 1 FROM producto p WHERE p.categoria_id = categoria.id)";

    /**
     * @param exito        true si el escenario terminó como se esperaba
     * @param pasos        descripción de cada paso ejecutado
     * @param errorEsperado mensaje del error provocado (solo escenario B)
     */
    public record Escenario(boolean exito, List<String> pasos, String errorEsperado) { }

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    /** Busca, desde 9001, los primeros IDs de categoría que no existen. */
    public List<Integer> elegirIdsLibres(int cuantos) throws SQLException {
        List<Integer> libres = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion()) {
            for (int id = PRIMER_ID_PRUEBA; libres.size() < cuantos; id++) {
                if (!categoriaDAO.existe(con, id)) {
                    libres.add(id);
                }
            }
        }
        return libres;
    }

    /** @return nombre de la categoría de prueba; incluye el ID, así nunca choca con el UNIQUE de nombre */
    public static String nombrePrueba(int id) { return PREFIJO_NOMBRE + id; }

    /** Escenario A: dos INSERT en la misma transacción y commit(). */
    public Escenario escenarioCommit(int id1, int id2) throws SQLException {
        List<String> pasos = new ArrayList<>();
        try (Connection con = ConexionBD.obtenerConexion()) {
            boolean autoCommitPrevio = con.getAutoCommit();
            try {
                con.setAutoCommit(false);
                pasos.add("setAutoCommit(false): empieza la transacción.");
                categoriaDAO.insertar(con, new Categoria(id1, nombrePrueba(id1)));
                pasos.add("INSERT categoría " + id1 + " (pendiente de confirmar).");
                categoriaDAO.insertar(con, new Categoria(id2, nombrePrueba(id2)));
                pasos.add("INSERT categoría " + id2 + " (pendiente de confirmar).");
                con.commit();
                pasos.add("commit(): los dos INSERT quedan guardados a la vez.");
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(autoCommitPrevio);
                pasos.add("setAutoCommit(" + autoCommitPrevio + "): se restaura el modo anterior.");
            }
        }
        // Se verifica con OTRA conexión: si la ve, es que el commit es real y visible para todos.
        boolean ambas = categoriaDAO.buscarPorId(id1).isPresent() && categoriaDAO.buscarPorId(id2).isPresent();
        pasos.add("SELECT con una conexión nueva: categoría " + id1 + " y " + id2
                + (ambas ? " existen." : " NO aparecen."));
        return new Escenario(ambas, pasos, null);
    }

    /** Escenario B: un INSERT correcto, un segundo INSERT duplicado a propósito y rollback(). */
    public Escenario escenarioRollback(int id) throws SQLException {
        List<String> pasos = new ArrayList<>();
        String error = null;
        try (Connection con = ConexionBD.obtenerConexion()) {
            boolean autoCommitPrevio = con.getAutoCommit();
            try {
                con.setAutoCommit(false);
                pasos.add("setAutoCommit(false): empieza otra transacción.");
                categoriaDAO.insertar(con, new Categoria(id, nombrePrueba(id)));
                pasos.add("INSERT categoría " + id + " (correcto, pendiente de confirmar).");
                pasos.add("INSERT categoría " + id + " otra vez: clave primaria duplicada a propósito...");
                categoriaDAO.insertar(con, new Categoria(id, nombrePrueba(id) + " duplicada"));
                con.commit(); // no debería llegar aquí
                pasos.add("commit() ejecutado (no esperado).");
            } catch (SQLException ex) {
                error = ErroresBD.explicar(ex) + " [código MySQL " + ex.getErrorCode() + "]";
                con.rollback();
                pasos.add("SQLException capturada -> rollback(): se deshace también el primer INSERT.");
            } finally {
                con.setAutoCommit(autoCommitPrevio);
                pasos.add("setAutoCommit(" + autoCommitPrevio + "): se restaura el modo anterior.");
            }
        }
        boolean noQueda = categoriaDAO.buscarPorId(id).isEmpty();
        pasos.add("SELECT con una conexión nueva: la categoría " + id
                + (noQueda ? " NO existe. El rollback ha funcionado." : " sigue existiendo (rollback fallido)."));
        return new Escenario(error != null && noQueda, pasos, error);
    }

    /**
     * Borra SOLO categorías de demostración: ID indicado, nombre "DEMO-TX ..." y sin productos.
     * @return número de categorías borradas
     */
    public int limpiar(List<Integer> ids) throws SQLException {
        int borradas = 0;
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(BORRAR_DEMO)) {
            for (int id : ids) {
                ps.setInt(1, id);
                borradas += ps.executeUpdate();
            }
        }
        return borradas;
    }
}
