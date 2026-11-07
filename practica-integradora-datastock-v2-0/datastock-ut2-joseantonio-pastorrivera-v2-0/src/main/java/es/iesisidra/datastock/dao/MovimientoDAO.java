package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.MovimientoStock;
import es.iesisidra.datastock.modelo.MovimientoStock.TipoMovimiento;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF-JDBC-05 y RF-JDBC-06 · Movimientos de stock.
 * <p>Las entradas y salidas NO hacen UPDATE desde Java: llaman al procedimiento
 * almacenado {@code registrar_movimiento}, que dentro de su propia transacción
 * bloquea la fila (SELECT ... FOR UPDATE), comprueba el stock, actualiza el
 * producto e inserta el movimiento. Por eso aquí no se usa setAutoCommit(false):
 * una transacción exterior interferiría con el COMMIT/ROLLBACK del procedimiento.</p>
 */
public class MovimientoDAO {
    /** Sintaxis de escape JDBC para llamar a un procedimiento: la traduce cada driver. */
    private static final String LLAMADA = "{call registrar_movimiento(?, ?, ?)}";
    private static final String COLUMNAS =
            "SELECT id, producto_codigo, tipo, cantidad, fecha, stock_anterior, stock_posterior FROM movimiento_stock ";
    private static final String TODOS = COLUMNAS + "ORDER BY fecha, id";
    private static final String POR_PRODUCTO = COLUMNAS + "WHERE producto_codigo = ? ORDER BY fecha, id";
    private static final String ULTIMO = COLUMNAS + "WHERE producto_codigo = ? ORDER BY id DESC LIMIT 1";

    /**
     * Registra una entrada o salida con CallableStatement y devuelve el movimiento creado.
     * @throws SQLException 1644 con el mensaje del SIGNAL ("Stock insuficiente",
     *         "Producto inexistente", "Tipo o cantidad invalidos"); en ese caso el
     *         procedimiento ya ha hecho ROLLBACK y no ha cambiado nada
     */
    public MovimientoStock registrar(String codigo, TipoMovimiento tipo, int cantidad) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            try (CallableStatement cs = con.prepareCall(LLAMADA)) {
                cs.setString(1, codigo);
                cs.setString(2, tipo.name());
                cs.setInt(3, cantidad);
                cs.execute();
            }
            // El procedimiento ya ha hecho COMMIT: consultamos el movimiento que acaba de insertar.
            return ultimoDe(con, codigo).orElseThrow(
                    () -> new SQLException("El procedimiento terminó sin registrar el movimiento."));
        }
    }

    public List<MovimientoStock> listarTodos() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(TODOS);
             ResultSet rs = ps.executeQuery()) {
            return leerTodos(rs);
        }
    }

    public List<MovimientoStock> listarPorProducto(String codigo) throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion();
             PreparedStatement ps = con.prepareStatement(POR_PRODUCTO)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return leerTodos(rs);
            }
        }
    }

    private Optional<MovimientoStock> ultimoDe(Connection con, String codigo) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(ULTIMO)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(leer(rs)) : Optional.empty();
            }
        }
    }

    private List<MovimientoStock> leerTodos(ResultSet rs) throws SQLException {
        List<MovimientoStock> movimientos = new ArrayList<>();
        while (rs.next()) {
            movimientos.add(leer(rs));
        }
        return movimientos;
    }

    /** DATETIME se lee directamente como LocalDateTime con getObject (JDBC 4.2). */
    private MovimientoStock leer(ResultSet rs) throws SQLException {
        return new MovimientoStock(rs.getLong("id"), rs.getString("producto_codigo"),
                TipoMovimiento.valueOf(rs.getString("tipo")), rs.getInt("cantidad"),
                rs.getObject("fecha", LocalDateTime.class), rs.getInt("stock_anterior"),
                rs.getInt("stock_posterior"));
    }
}
