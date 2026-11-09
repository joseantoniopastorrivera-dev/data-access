// Ejercicio 1 NIO: Pide al usuario una ruta de fichero y muestra:
// 1. Nombre del fichero
// 2. Directorio padre
// 3. Si existe o no
// 4. Si es un fichero o directorio
// 5. Su tamaño en bytes (si existe y es un fichero)
// Autor: JAPR
// Fecha: 22/09/2026

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Ejercicio1NIO {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Introduce la ruta de un fichero o directorio: ");
        String rutaStr = scanner.nextLine();

        // En NiO.2 usamos 'Path' y no 'File' para representar rutas
        Path ruta = Paths.get(rutaStr);

        // 1. Nombre del fichero o directorio
        System.out.println("Nombre: " + ruta.getFileName());

        // 2. Directorio padre
        Path padre = ruta.getParent();
        System.out.println("Directorio padre: " + (padre != null ? padre : "No tiene directorio."));

        // 3. Comprobar si existe o no
        if (Files.exists(ruta)) {
            System.out.println("El fichero o directorio SÍ existe.");
        }

        // 4. Comprobar si es fichero o directorio
        if (Files.isDirectory(ruta)) {
            System.out.println("Tipo: DIRECTORIO.");
        } else if (Files.isRegularFile(ruta)) {
            System.out.println("Tipo: FICHERO.");

            // 5.Tamaño en bytes
            try {
                long tamano = Files.size(ruta);
                System.out.println("Tamaño: " + tamano + " bytes.");
            } catch (IOException e) {
                System.out.println("No se pudo leer el tamaño: " + e.getMessage());
            }
        } else {
            System.out.println("El fichero o directorio NO existe.");
        }

        scanner.close();

    }
}