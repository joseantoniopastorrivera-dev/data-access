package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.servicio.Inventario;

/**
 * Operación común de los importadores de catálogos (JSON y XML).
 * <p>Gracias a esta interfaz, el menú trata ambos formatos igual y solo
 * cambia el objeto que recibe.</p>
 */
public interface ImportadorCatalogo {

    /**
     * Lee el fichero y añade al inventario los productos válidos y no duplicados.
     * @param textoRuta  ruta del fichero escrita por el usuario
     * @param inventario colección común de productos
     * @return resumen de la importación (el documento se pudo leer)
     * @throws ImportacionException si el documento entero no puede procesarse; el inventario no cambia
     */
    ResultadoImportacion importar(String textoRuta, Inventario inventario) throws ImportacionException;
}
