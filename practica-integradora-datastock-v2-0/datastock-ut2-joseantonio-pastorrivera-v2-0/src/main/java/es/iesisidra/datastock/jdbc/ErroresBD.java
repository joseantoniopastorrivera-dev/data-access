package es.iesisidra.datastock.jdbc;

import es.iesisidra.datastock.config.ConexionBD;
import java.sql.SQLException;

/**
 * Traduce una {@link SQLException} a un mensaje comprensible para el usuario.
 * <p>Se decide por el código de error de MySQL ({@code getErrorCode()}) y por el
 * SQLState, nunca mostrando la traza técnica ni datos sensibles. Los mensajes de
 * MySQL no incluyen la contraseña, pero aun así solo se muestran literalmente
 * los que lanza el procedimiento con SIGNAL, que hemos escrito nosotros.</p>
 */
public final class ErroresBD {
    private ErroresBD() { }

    /** @return explicación en español del error recibido */
    public static String explicar(SQLException ex) {
        if (ConexionBD.ESTADO_SIN_CONFIGURACION.equals(ex.getSQLState())) {
            return ex.getMessage();
        }
        String mensaje = ex.getMessage() == null ? "" : ex.getMessage();
        if (mensaje.contains("No suitable driver")) {
            return "No se encuentra el driver JDBC de MySQL. Revisa la dependencia mysql-connector-j y el classpath.";
        }
        switch (ex.getErrorCode()) {
            case 1045: return "Acceso denegado: el usuario o la contraseña de MySQL no son correctos.";
            // 1044: el usuario no tiene permisos sobre ese esquema. MySQL no revela si existe o no,
            // así que con un usuario de mínimo privilegio un esquema mal escrito llega como 1044.
            case 1044: return "El usuario de MySQL no tiene acceso a esa base de datos: comprueba el nombre del esquema en la URL (debe ser datastock) y los permisos del usuario.";
            case 1049: return "La base de datos indicada en la URL no existe. Ejecuta material-referencia/datastock-bd-mysql.sql.";
            case 1146: return "Falta una tabla del esquema datastock. Ejecuta material-referencia/datastock-bd-mysql.sql.";
            case 1305: return "No existe el procedimiento registrar_movimiento. Ejecuta el script SQL completo.";
            case 1062: return "Clave duplicada: ya existe un registro con ese valor, que debe ser único.";
            case 1451, 1217: return "Operación bloqueada por integridad referencial: hay registros que dependen de este.";
            case 1452, 1216: return "Operación bloqueada por integridad referencial: la categoría o el proveedor indicado no existe.";
            case 1406: return "Un texto supera la longitud máxima permitida por la columna.";
            case 3819: return "Un dato no cumple una restricción CHECK de la tabla.";
            case 1644: return mensaje; // SIGNAL del procedimiento: "Stock insuficiente", "Producto inexistente"...
            default: break;
        }
        String estado = ex.getSQLState();
        if (estado != null && estado.startsWith("08")) {
            return "No se puede conectar con el servidor MySQL. Comprueba que está arrancado y que el host y el puerto de la URL son correctos.";
        }
        return "Error de base de datos (código " + ex.getErrorCode() + ", SQLState " + estado + ").";
    }

    /** @return true si el error es de clave duplicada (PRIMARY KEY o UNIQUE) */
    public static boolean esDuplicado(SQLException ex) { return ex.getErrorCode() == 1062; }

    /**
     * @return true si el error lo provoca una clave foránea (al borrar o al insertar).
     * <p>MySQL tiene dos versiones de cada error: 1451/1452 incluyen el nombre de la
     * restricción y 1217/1216 no. Cuál llega depende de los privilegios del usuario
     * (con datastock_app llega 1217), así que se aceptan las cuatro. Además, todas
     * comparten el SQLState 23000 (violación de integridad).</p>
     */
    public static boolean esClaveForanea(SQLException ex) {
        int codigo = ex.getErrorCode();
        return codigo == 1451 || codigo == 1452 || codigo == 1217 || codigo == 1216;
    }
}
