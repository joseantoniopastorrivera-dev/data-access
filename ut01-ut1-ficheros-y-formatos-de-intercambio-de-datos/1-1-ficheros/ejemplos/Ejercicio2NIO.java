//Ejercicio 2 NIO: Crea un programa que:
//1. cree un directorio llamado PruebasNIO si no existe
//2. Dentro de él, cree un fichero saludo.txt
//3. Escriba dentro del fichero:
//   -Hola desde NIO.2
//   -Hoy es <fecha actual>
//4. Lea después el fichero línea a línea y muestre su contenido en consola

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class Ejercicio2NIO {
    public static void main(String[] args) {
        // Definimos las rutas utilizando Path
        Path directorio = Paths.get("PruebasNIO");
        // Unimos la ruta de forma segura
        Path fichero = directorio.resolve("saludo.txt");

        try {
            // 1. Crear el directorio si no existe
            if (Files.notExists(directorio)) {
                Files.createDirectory(directorio);
                System.out.println("Directorio 'PruebasNIO' creado con éxito.");
            }

            // 2 y 3. Crear el fichero y escribir las líneas de texto con codificación UTF-8
            String fechaActual = LocalDate.now().toString();
            List<String> lineasAEscribir = List.of(
                    "Hola desde NIO.2",
                    "Hoy es " + fechaActual);

            // Files.write escribe colecciones de strings directamente manejando flujos de
            // bytes/caracteres de forma interna
            Files.write(fichero, lineasAEscribir, StandardCharsets.UTF_8);
            System.out.println("Fichero 'saludo.txt' escrito correctamente.");

            // 4. Leer después el fichero línea a línea
            System.out.println("\n---CONTENDIDO LEÍDO DEL FICHERO---");
            List<String> lineasLeidas = Files.readAllLines(fichero, StandardCharsets.UTF_8);
            for (String linea : lineasLeidas) {
                System.out.println(linea);
            }

        } catch (IOException e) {
            System.out.println("Ocurrió un error E/S: " + e.getMessage());
        }
    }
}