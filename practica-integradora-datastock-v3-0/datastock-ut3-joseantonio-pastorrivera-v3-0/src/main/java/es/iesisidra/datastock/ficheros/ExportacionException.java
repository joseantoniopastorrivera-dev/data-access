package es.iesisidra.datastock.ficheros;

/**
 * Error que impide generar un fichero de salida: ruta de destino inválida,
 * directorio inexistente, falta de permisos o fallo de escritura.
 * <p>Cuando se lanza, el fichero de destino que ya existiera no se ha modificado.</p>
 */
public class ExportacionException extends Exception {

    public ExportacionException(String mensaje) {
        super(mensaje);
    }

    public ExportacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
