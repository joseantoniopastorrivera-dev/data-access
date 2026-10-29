package es.iesisidra.datastock.ficheros;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Comprobaciones de rutas compartidas por las operaciones que leen ficheros
 * (importar JSON, importar XML y, más adelante, recuperar el inventario).
 */
public final class ComprobadorRutas {
    private ComprobadorRutas() { }

    /**
     * Comprueba que la ruta existe, es un fichero regular y se puede leer.
     * @param textoRuta ruta escrita por el usuario
     * @return la ruta convertida en Path
     * @throws ImportacionException con un mensaje claro si no se puede leer
     */
    public static Path ficheroLegible(String textoRuta) throws ImportacionException {
        if (textoRuta == null || textoRuta.isBlank()) {
            throw new ImportacionException("Debes indicar la ruta del fichero.");
        }
        Path ruta;
        try {
            ruta = Path.of(textoRuta.trim());
        } catch (InvalidPathException ex) {
            throw new ImportacionException("La ruta contiene caracteres no válidos: " + ex.getMessage(), ex);
        }
        if (!Files.exists(ruta)) {
            throw new ImportacionException("El fichero no existe: " + ruta.toAbsolutePath());
        }
        if (!Files.isRegularFile(ruta)) {
            throw new ImportacionException("La ruta no es un fichero (¿es un directorio?): " + ruta.toAbsolutePath());
        }
        if (!Files.isReadable(ruta)) {
            throw new ImportacionException("No tienes permisos para leer el fichero: " + ruta.toAbsolutePath());
        }
        return ruta;
    }
}
