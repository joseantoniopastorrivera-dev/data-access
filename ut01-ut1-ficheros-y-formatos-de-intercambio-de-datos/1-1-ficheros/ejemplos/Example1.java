//Codigo escrito por Ruth en Clase 22/09/2026

import java.io.File;

public class Example1 {
    public static void main(String[] args) {

        File ruta = new File("datos");
        if (!ruta.exists()) {
            System.out.println("La ruta no existe.");
            System.out.println("Ruta buscada: " + ruta.getName());

        } else {
            System.out.println("Nombre: " + ruta.getName());
            System.out.println("Ruta:" + ruta.getPath());
            System.out.println("Ruta Absoluta: " + ruta.getAbsolutePath());
            System.out.println("Directorio Padre: " + ruta.getParent());

            if (ruta.isFile()) {
                System.out.println("Tipo: FICHERO.");
                System.out.println("Tamaño: " + ruta.length() + " bytes");
            }

            if (ruta.isDirectory()) {
                System.out.println("Tipo: DIRECTORIO.");
                File[] contenido = ruta.listFiles();
                if (contenido != null) {
                    System.out.println("---CONTENIDO DEL DIRECTORIO---");
                    for (File elemento : contenido) {
                        if (elemento.isDirectory()) {

                        }

                    }
                }
            }
        }
    }
}