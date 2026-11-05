package es.iesisidra.datastock.ficheros;

/**
 * Error que impide procesar un documento completo: fichero inaccesible,
 * sintaxis incorrecta o estructura raíz inválida.
 * <p>Cuando se lanza, no se ha añadido ningún producto al inventario.</p>
 */
public class ImportacionException extends Exception {

    public ImportacionException(String mensaje) {
        super(mensaje);
    }

    public ImportacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
