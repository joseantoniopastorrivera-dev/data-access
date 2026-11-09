package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.util.ColoresConsola;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Actividad comparativa obligatoria (apartado 7): SQLite, gestor EMBEBIDO.
 * <p>Se ejecuta por separado, con su propio {@code main}; no está en los menús
 * de DataStock, que sigue usando MySQL. SQLite no necesita servidor: la base de
 * datos es un fichero local que el driver crea al conectar.</p>
 * <p>Usa la misma API JDBC que MySQL (Connection, Statement, PreparedStatement,
 * ResultSet): solo cambian la dependencia y la URL.</p>
 */
public final class DemoSQLite {
    private static final Path CARPETA = Path.of("datos", "sqlite");
    private static final Path FICHERO = CARPETA.resolve("productos_demo.db");
    /** Sin usuario ni contraseña: no hay servidor al que autenticarse. */
    private static final String URL = "jdbc:sqlite:" + FICHERO.toString().replace('\\', '/');

    private static final String CREAR_TABLA = "CREATE TABLE IF NOT EXISTS producto_demo ("
            + "codigo TEXT PRIMARY KEY NOT NULL, nombre TEXT NOT NULL)";
    /** INSERT OR IGNORE: si el código ya existe, SQLite no inserta y devuelve 0 filas, sin error. */
    private static final String INSERTAR = "INSERT OR IGNORE INTO producto_demo (codigo, nombre) VALUES (?, ?)";
    private static final String CONSULTAR = "SELECT codigo, nombre FROM producto_demo ORDER BY codigo";
    private static final String[][] DATOS = {{"SQL-001", "Producto demo A"}, {"SQL-002", "Producto demo B"}};

    private DemoSQLite() { }

    public static void main(String[] args) {
        try {
            Files.createDirectories(CARPETA); // SQLite crea el fichero, pero no las carpetas
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("[ERROR] No se puede crear " + CARPETA + ": " + ex.getMessage(), ColoresConsola.ROJO));
            return;
        }
        boolean existiaAntes = Files.exists(FICHERO);

        try (Connection con = DriverManager.getConnection(URL)) {
            ok("Conexión SQLite abierta: " + FICHERO.toString().replace('\\', '/')
                    + (existiaAntes ? " (fichero existente)" : " (fichero creado ahora)"));

            try (Statement st = con.createStatement()) {
                st.execute(CREAR_TABLA);
            }
            ok("Tabla producto_demo disponible.");

            int insertados = 0;
            try (PreparedStatement ps = con.prepareStatement(INSERTAR)) {
                for (String[] fila : DATOS) {
                    ps.setString(1, fila[0]);
                    ps.setString(2, fila[1]);
                    insertados += ps.executeUpdate(); // 1 si lo inserta, 0 si ya existía
                }
            }
            System.out.println("Insertados: " + insertados + " | Ya existentes: " + (DATOS.length - insertados));

            int filas = 0;
            try (PreparedStatement ps = con.prepareStatement(CONSULTAR);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getString("codigo") + " | " + rs.getString("nombre"));
                    filas++;
                }
            }
            System.out.println("Filas en producto_demo: " + filas);
        } catch (SQLException ex) {
            System.out.println(ColoresConsola.pintar("[ERROR] SQLite: " + ex.getMessage(), ColoresConsola.ROJO));
            return;
        }
        // Al salir del try-with-resources la conexión ya está cerrada; el fichero sigue en disco.
        try {
            ok("Recursos cerrados. El fichero persiste: " + FICHERO.toAbsolutePath() + " (" + Files.size(FICHERO) + " bytes)");
        } catch (IOException ex) {
            System.out.println("No se pudo leer el tamaño del fichero: " + ex.getMessage());
        }
    }

    private static void ok(String texto) {
        System.out.println(ColoresConsola.pintar("[OK] " + texto, ColoresConsola.VERDE));
    }
}
