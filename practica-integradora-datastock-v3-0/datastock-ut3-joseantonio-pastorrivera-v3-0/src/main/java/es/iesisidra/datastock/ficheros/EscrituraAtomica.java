package es.iesisidra.datastock.ficheros;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Escribe un fichero sin riesgo de dejarlo a medias.
 * <p>El contenido se escribe primero en un fichero temporal del mismo directorio
 * y, solo si todo ha ido bien, se mueve sobre el destino. Si la escritura falla,
 * el fichero original (si existía) queda intacto. Lo usan las exportaciones
 * (RF-FIC-05) y el guardado del inventario (RF-FIC-06).</p>
 */
public final class EscrituraAtomica {
    private EscrituraAtomica() { }

    /** Código que vuelca el contenido en el fichero temporal que recibe. */
    @FunctionalInterface
    public interface Contenido {
        void escribirEn(Path temporal) throws Exception;
    }

    /**
     * @param destino   fichero final (su directorio debe existir)
     * @param contenido código que escribe los datos en el temporal
     * @throws ExportacionException si no se puede escribir; el destino no se modifica
     */
    public static void escribir(Path destino, Contenido contenido) throws ExportacionException {
        Path directorio = destino.toAbsolutePath().getParent();
        Path temporal = null;
        try {
            temporal = Files.createTempFile(directorio, ".datastock-", ".tmp");
            contenido.escribirEn(temporal);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AccessDeniedException ex) {
            throw new ExportacionException("No tienes permisos para escribir en " + directorio, ex);
        } catch (Exception ex) {
            throw new ExportacionException("No se pudo escribir el fichero: " + ex.getMessage(), ex);
        } finally {
            borrarSiQueda(temporal);
        }
    }

    /** Tras un fallo, elimina el temporal para no dejar basura en el directorio. */
    private static void borrarSiQueda(Path temporal) {
        if (temporal == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException ex) {
            // El error importante ya se ha comunicado; solo avisamos de que queda un temporal.
            System.err.println("Aviso: no se pudo borrar el fichero temporal " + temporal);
        }
    }
}
