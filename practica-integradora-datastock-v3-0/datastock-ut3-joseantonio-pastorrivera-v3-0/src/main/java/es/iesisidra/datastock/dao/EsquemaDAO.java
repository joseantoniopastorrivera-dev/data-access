package es.iesisidra.datastock.dao;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dto.InfoBaseDatos;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * RF-JDBC-01 · Comprueba la conexión y que el esquema del script SQL está disponible.
 */
public class EsquemaDAO {
    private static final List<String> TABLAS = List.of("categoria", "proveedor", "producto", "movimiento_stock");

    /**
     * Abre una conexión, lee los metadatos y cuenta los productos.
     * <p>Todo va dentro de try-with-resources: la conexión se cierra al salir,
     * tanto si todo va bien como si salta una excepción.</p>
     */
    public InfoBaseDatos comprobar() throws SQLException {
        try (Connection con = ConexionBD.obtenerConexion()) {
            DatabaseMetaData meta = con.getMetaData();
            String esquema = con.getCatalog();

            List<String> faltan = new ArrayList<>();
            for (String tabla : TABLAS) {
                try (ResultSet rs = meta.getTables(esquema, null, tabla, new String[] {"TABLE"})) {
                    if (!rs.next()) {
                        faltan.add(tabla);
                    }
                }
            }
            boolean procedimiento;
            try (ResultSet rs = meta.getProcedures(esquema, null, "registrar_movimiento")) {
                procedimiento = rs.next();
            }
            // Consulta sencilla de acceso a datos. Sin datos del usuario: basta con Statement.
            long productos = -1;
            if (!faltan.contains("producto")) {
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM producto")) {
                    rs.next();
                    productos = rs.getLong(1);
                }
            }
            return new InfoBaseDatos(esquema, meta.getDatabaseProductName(), meta.getDatabaseProductVersion(),
                    meta.getDriverName() + " " + meta.getDriverVersion(), faltan, procedimiento, productos);
        }
    }
}
