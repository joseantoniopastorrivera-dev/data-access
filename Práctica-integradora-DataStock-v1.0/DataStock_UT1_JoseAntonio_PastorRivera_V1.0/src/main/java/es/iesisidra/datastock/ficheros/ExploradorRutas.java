package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.util.ColoresConsola;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * RF-FIC-01 · Explorar rutas, ficheros y directorios.
 * <p>Recibe una ruta escrita por el usuario e informa de si existe y de si es
 * un fichero o un directorio. Con un directorio muestra solo su contenido
 * inmediato, sin recorrer los subdirectorios.</p>
 * <p>Se usa NIO ({@link Path} y {@link Files}) porque los errores llegan como
 * excepciones concretas en lugar de valores especiales como null o 0.</p>
 */
public class ExploradorRutas {

    /**
     * Analiza la ruta indicada y muestra el resultado por consola.
     * @param textoRuta ruta tal y como la escribe el usuario (relativa o absoluta)
     */
    public void explorar(String textoRuta) {
        if (textoRuta == null || textoRuta.isBlank()) {
            error("Debes indicar una ruta.");
            return;
        }

        Path ruta;
        try {
            ruta = Path.of(textoRuta.trim());
        } catch (InvalidPathException ex) {
            error("La ruta contiene caracteres no válidos: " + ex.getMessage());
            return;
        }

        if (!Files.exists(ruta)) {
            error("La ruta no existe: " + ruta.toAbsolutePath());
            return;
        }

        if (Files.isRegularFile(ruta)) {
            mostrarFichero(ruta);
        } else if (Files.isDirectory(ruta)) {
            mostrarDirectorio(ruta);
        } else {
            System.out.println("La ruta existe, pero no es un fichero normal ni un directorio.");
        }
    }

    /** Muestra nombre, ruta absoluta y tamaño de un fichero regular. */
    private void mostrarFichero(Path fichero) {
        try {
            long tamano = Files.size(fichero);
            System.out.println("[FICHERO] " + fichero.getFileName());
            System.out.println("Ruta absoluta: " + fichero.toAbsolutePath());
            System.out.println("Tamaño: " + tamano + " bytes");
        } catch (IOException ex) {
            error("No se pudo leer la información del fichero: " + ex.getMessage());
        }
    }

    /** Lista el contenido inmediato de un directorio y muestra los totales. */
    private void mostrarDirectorio(Path directorio) {
        List<Path> elementos;
        try (Stream<Path> contenido = Files.list(directorio)) {
            elementos = contenido.sorted().toList();
        } catch (AccessDeniedException ex) {
            error("No tienes permisos para leer el directorio: " + directorio.toAbsolutePath());
            return;
        } catch (IOException ex) {
            error("Error al leer el directorio: " + ex.getMessage());
            return;
        }

        System.out.println("Directorio: " + directorio.toAbsolutePath());
        if (elementos.isEmpty()) {
            System.out.println("Directorio vacío.");
        }

        int ficheros = 0;
        int directorios = 0;
        for (Path elemento : elementos) {
            String nombre = elemento.getFileName().toString();
            if (Files.isDirectory(elemento)) {
                System.out.println("  [DIR]     " + nombre);
                directorios++;
            } else {
                System.out.println("  [FICHERO] " + nombre + "  " + tamanoTexto(elemento));
                ficheros++;
            }
        }
        System.out.println("Total ficheros: " + ficheros + " | Directorios: " + directorios);
    }

    /** Devuelve el tamaño como texto; si no puede leerse, lo indica sin interrumpir el listado. */
    private String tamanoTexto(Path fichero) {
        try {
            return Files.size(fichero) + " bytes";
        } catch (IOException ex) {
            return "(tamaño no disponible)";
        }
    }

    private void error(String mensaje) {
        System.out.println(ColoresConsola.pintar(mensaje, ColoresConsola.ROJO));
    }
}
