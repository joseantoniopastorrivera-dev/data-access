package es.iesisidra.datastock.ficheros;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Comprobaciones de rutas compartidas por las operaciones que leen ficheros
 * (importar JSON y XML, recuperar el inventario) y por las que los escriben
 * (exportar y guardar el inventario).
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

    /**
     * Comprueba que se puede crear o sobrescribir un fichero en la ruta indicada:
     * su directorio debe existir y la ruta no puede ser un directorio.
     * No pregunta nada al usuario: la confirmación de sobrescritura es cosa del menú.
     * @param textoRuta ruta escrita por el usuario
     * @return la ruta convertida en Path
     * @throws ExportacionException con un mensaje claro si no es un destino válido
     */
    public static Path destinoEscribible(String textoRuta) throws ExportacionException {
        if (textoRuta == null || textoRuta.isBlank()) {
            throw new ExportacionException("Debes indicar la ruta de destino.");
        }
        Path ruta;
        try {
            ruta = Path.of(textoRuta.trim());
        } catch (InvalidPathException ex) {
            throw new ExportacionException("La ruta contiene caracteres no válidos: " + ex.getMessage(), ex);
        }
        if (Files.isDirectory(ruta)) {
            throw new ExportacionException("La ruta es un directorio; indica también el nombre del fichero: "
                    + ruta.toAbsolutePath());
        }
        Path directorio = ruta.toAbsolutePath().getParent();
        if (directorio == null || !Files.isDirectory(directorio)) {
            throw new ExportacionException("El directorio de destino no existe: " + directorio);
        }
        if (!Files.isWritable(directorio)) {
            throw new ExportacionException("No tienes permisos para escribir en: " + directorio);
        }
        return ruta;
    }
}
