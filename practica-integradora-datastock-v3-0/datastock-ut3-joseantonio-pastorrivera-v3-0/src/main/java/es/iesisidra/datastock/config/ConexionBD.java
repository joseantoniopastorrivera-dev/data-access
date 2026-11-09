package es.iesisidra.datastock.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica de conexiones JDBC a MySQL (UT2, apartado 4).
 * <p>Los datos de conexión NUNCA están en el código. Cada valor se busca primero
 * en una variable de entorno ({@code JDBC_URL}, {@code JDBC_USER},
 * {@code JDBC_PASSWORD}) y, si no existe, en el fichero local
 * {@code config/db.properties}, que está excluido de Git. En el repositorio solo
 * se sube la plantilla {@code config/db.properties.example}.</p>
 * <p>Cada operación abre una conexión corta con {@link #obtenerConexion()} y la
 * cierra con try-with-resources; por eso no se necesita un pool de conexiones.</p>
 */
public final class ConexionBD {
    /** SQLState propio para distinguir "falta configuración" de los errores del servidor. */
    public static final String ESTADO_SIN_CONFIGURACION = "DS001";

    private static final Path FICHERO = Path.of("config", "db.properties");
    /** Segundos máximos de espera al conectar: la aplicación no se queda colgada si MySQL no responde. */
    private static final int ESPERA_MAXIMA_SEGUNDOS = 5;

    private ConexionBD() { }

    /**
     * Abre una conexión nueva. Quien la pide es responsable de cerrarla
     * (siempre con try-with-resources).
     * @throws SQLException si falta configuración, el servidor no responde,
     *         las credenciales son incorrectas o el esquema no existe
     */
    public static Connection obtenerConexion() throws SQLException {
        DatosConexion datos = cargarDatos();
        DriverManager.setLoginTimeout(ESPERA_MAXIMA_SEGUNDOS);
        return DriverManager.getConnection(datos.url(), datos.usuario(), datos.contrasena());
    }

    /** @return de dónde se han leído los datos, para informar al usuario sin mostrar secretos */
    public static String origenConfiguracion() {
        boolean entorno = System.getenv("JDBC_URL") != null || System.getenv("JDBC_USER") != null
                || System.getenv("JDBC_PASSWORD") != null;
        boolean fichero = Files.isRegularFile(FICHERO);
        if (entorno && fichero) {
            return "variables de entorno y " + FICHERO;
        }
        return entorno ? "variables de entorno" : FICHERO.toString();
    }

    /**
     * Reúne URL, usuario y contraseña. Cada valor sale de su variable de entorno
     * o, si no está definida, de config/db.properties.
     */
    private static DatosConexion cargarDatos() throws SQLException {
        Properties fichero = leerFichero();
        String url = valor("JDBC_URL", "jdbc.url", fichero);
        String usuario = valor("JDBC_USER", "jdbc.user", fichero);
        String contrasena = valor("JDBC_PASSWORD", "jdbc.password", fichero);
        if (url == null || url.isBlank() || usuario == null || usuario.isBlank() || contrasena == null) {
            throw new SQLException("Faltan datos de conexión. Define JDBC_URL, JDBC_USER y JDBC_PASSWORD "
                    + "o copia config/db.properties.example como config/db.properties y complétalo.",
                    ESTADO_SIN_CONFIGURACION);
        }
        return new DatosConexion(url.trim(), usuario.trim(), contrasena);
    }

    /** @return las propiedades del fichero local, o un Properties vacío si el fichero no existe */
    private static Properties leerFichero() throws SQLException {
        Properties propiedades = new Properties();
        if (!Files.isRegularFile(FICHERO)) {
            return propiedades;
        }
        try (Reader lector = Files.newBufferedReader(FICHERO, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
            return propiedades;
        } catch (IOException ex) {
            throw new SQLException("No se ha podido leer " + FICHERO + ": " + ex.getMessage(),
                    ESTADO_SIN_CONFIGURACION);
        }
    }

    private static String valor(String variableEntorno, String clave, Properties fichero) {
        String desdeEntorno = System.getenv(variableEntorno);
        return desdeEntorno != null ? desdeEntorno : fichero.getProperty(clave);
    }

    /**
     * Datos de conexión agrupados. Se redefine toString() porque el de un record
     * mostraría la contraseña si alguien imprimiera el objeto por error.
     */
    private record DatosConexion(String url, String usuario, String contrasena) {
        @Override public String toString() {
            return "DatosConexion[url=" + url + ", usuario=" + usuario + ", contrasena=****]";
        }
    }
}
