package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.ficheros.ImportadorCatalogo;
import es.iesisidra.datastock.ficheros.ImportadorJson;
import es.iesisidra.datastock.ficheros.ImportadorXml;
import es.iesisidra.datastock.ficheros.ResultadoImportacion;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * RF-FIC-08 · Actividad independiente de acceso aleatorio con RandomAccessFile.
 * <p>No forma parte del menú principal. Como tiene su propio main, tiene
 * también su propio inventario en memoria: se llena importando un catálogo
 * JSON o XML con los mismos importadores de DataStock, y a partir de él se
 * genera el fichero binario. La lógica del fichero está en {@link FicheroStock}.</p>
 * <p>Ejecución: {@code mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.AccesoAleatorioActividad}</p>
 */
public final class AccesoAleatorioActividad {
    private AccesoAleatorioActividad() { }

    private static final Inventario INVENTARIO = new Inventario();

    public static void main(String[] args) {
        System.out.println("===== ACCESO ALEATORIO =====");
        FicheroStock fichero = pedirFichero();
        if (fichero == null) {
            return;
        }
        int opcion;
        do {
            System.out.println("\nFichero: " + fichero.getRuta() + " | Productos en memoria: " + INVENTARIO.tamano());
            System.out.println("1. Cargar catálogo JSON/XML en memoria | 2. Generar fichero desde memoria");
            System.out.println("3. Consultar posición | 4. Modificar stock | 5. Mostrar todos | 0. Salir");
            opcion = LectorConsola.leerOpcion("Opción: ", 0, 5);
            try {
                switch (opcion) {
                    case 1 -> cargarCatalogo();
                    case 2 -> generar(fichero);
                    case 3 -> consultar(fichero);
                    case 4 -> modificar(fichero);
                    case 5 -> mostrarTodos(fichero);
                    case 0 -> System.out.println("Fin de la actividad.");
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (IOException | IllegalArgumentException ex) {
                System.out.println(ColoresConsola.pintar(ex.getMessage(), ColoresConsola.ROJO));
            }
        } while (opcion != 0);
    }

    private static FicheroStock pedirFichero() {
        while (true) {
            String texto = LectorConsola.leerTexto("Fichero binario de stock (p. ej. datos/salidas/stock.bin; Intro para salir): ");
            if (texto.isBlank()) {
                return null;
            }
            try {
                Path ruta = Path.of(texto.trim());
                Path directorio = ruta.toAbsolutePath().getParent();
                if (Files.isDirectory(ruta) || directorio == null || !Files.isDirectory(directorio)) {
                    System.out.println(ColoresConsola.pintar("Indica un fichero dentro de un directorio existente.", ColoresConsola.AMARILLO));
                } else {
                    return new FicheroStock(ruta);
                }
            } catch (InvalidPathException ex) {
                System.out.println(ColoresConsola.pintar("Ruta no válida: " + ex.getMessage(), ColoresConsola.AMARILLO));
            }
        }
    }

    /** Llena el inventario de esta actividad con los importadores de DataStock. */
    private static void cargarCatalogo() {
        String ruta = LectorConsola.leerTexto("Catálogo JSON o XML: ").trim();
        ImportadorCatalogo importador = ruta.toLowerCase(Locale.ROOT).endsWith(".xml")
                ? new ImportadorXml() : new ImportadorJson();
        try {
            ResultadoImportacion resultado = importador.importar(ruta, INVENTARIO);
            System.out.println("Importados: " + resultado.getImportados() + " | Rechazados: " + resultado.getRechazados());
        } catch (ImportacionException ex) {
            System.out.println(ColoresConsola.pintar("No se ha cargado nada: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }

    private static void generar(FicheroStock fichero) throws IOException {
        if (INVENTARIO.estaVacio()) {
            System.out.println(ColoresConsola.pintar("No hay productos en memoria: carga primero un catálogo (opción 1). "
                    + "No se genera el fichero.", ColoresConsola.AMARILLO));
            return;
        }
        if (Files.exists(fichero.getRuta()) && !LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribirlo?")) {
            System.out.println("Generación cancelada.");
            return;
        }
        int registros = fichero.generar(INVENTARIO.listarOrdenado());
        System.out.println("Registros: " + registros + " | Tamaño: " + Files.size(fichero.getRuta()) + " bytes");
    }

    private static void consultar(FicheroStock fichero) throws IOException {
        int total = fichero.numeroRegistros();
        System.out.println("Registros: " + total + " | Tamaño: " + (long) total * FicheroStock.TAM_REGISTRO + " bytes");
        if (total == 0) {
            System.out.println("El fichero no contiene registros.");
            return;
        }
        int posicion = LectorConsola.leerEntero("Consultar posición (0-" + (total - 1) + "): ");
        mostrar(fichero.leer(posicion));
    }

    private static void modificar(FicheroStock fichero) throws IOException {
        int total = fichero.numeroRegistros();
        if (total == 0) {
            System.out.println("El fichero no contiene registros.");
            return;
        }
        int posicion = LectorConsola.leerEntero("Posición a modificar (0-" + (total - 1) + "): ");
        FicheroStock.RegistroStock registro = fichero.leer(posicion);
        mostrar(registro);
        int nuevo = LectorConsola.leerEntero("Nuevo stock: ");
        int anterior = fichero.modificarStock(posicion, nuevo);
        System.out.println(ColoresConsola.pintar("Actualización correcta: stock " + anterior + " → " + nuevo, ColoresConsola.VERDE));
        Optional<Producto> enMemoria = INVENTARIO.buscarPorCodigo(registro.codigo());
        enMemoria.ifPresent(p -> System.out.println("(El producto " + p.getCodigo()
                + " en memoria sigue con stock " + p.getStock() + ": el fichero es una copia independiente.)"));
    }

    private static void mostrarTodos(FicheroStock fichero) throws IOException {
        var registros = fichero.leerTodos();
        if (registros.isEmpty()) {
            System.out.println("El fichero no contiene registros.");
        }
        registros.forEach(AccesoAleatorioActividad::mostrar);
        System.out.println("Total: " + registros.size() + " registros");
    }

    private static void mostrar(FicheroStock.RegistroStock r) {
        System.out.printf(Locale.ROOT, "[%d] %-12s | %.2f € | stock %d%n", r.posicion(), r.codigo(), r.precio(), r.stock());
    }
}
