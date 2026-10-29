package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import java.nio.file.Path;
import java.util.List;

/**
 * Operación común de los exportadores de catálogos (JSON y XML).
 * <p>El intercambio entre formatos pasa siempre por la colección Java:
 * JSON → Producto → XML y XML → Producto → JSON. No hay conversor directo.</p>
 */
public interface ExportadorCatalogo {

    /**
     * Escribe los productos en el fichero de destino, en UTF-8 y en el orden recibido.
     * @param productos productos que se exportan (el llamante los pasa ordenados por código)
     * @param destino   fichero de salida; su directorio ya se ha comprobado
     * @throws ExportacionException si no se puede escribir; un fichero anterior no se modifica
     */
    void exportar(List<Producto> productos, Path destino) throws ExportacionException;
}
