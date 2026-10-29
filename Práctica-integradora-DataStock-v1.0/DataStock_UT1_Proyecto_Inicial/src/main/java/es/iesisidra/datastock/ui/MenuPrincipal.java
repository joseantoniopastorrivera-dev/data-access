package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.ficheros.ComprobadorRutas;
import es.iesisidra.datastock.ficheros.ExploradorRutas;
import es.iesisidra.datastock.ficheros.ExportacionException;
import es.iesisidra.datastock.ficheros.ExportadorCatalogo;
import es.iesisidra.datastock.ficheros.ExportadorJson;
import es.iesisidra.datastock.ficheros.ExportadorXml;
import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.ficheros.ImportadorCatalogo;
import es.iesisidra.datastock.ficheros.ImportadorJson;
import es.iesisidra.datastock.ficheros.ImportadorXml;
import es.iesisidra.datastock.ficheros.ResultadoImportacion;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Presentación de menús y navegación de la primera fase de DataStock.
 * <p>Los métodos marcados TODO son puntos de partida, NO soluciones de los
 * requisitos. El alumnado deberá delegar el acceso a ficheros, la validación
 * y el procesamiento en métodos o clases independientes.</p>
 */
public class MenuPrincipal {
    private final Inventario inventario;
    private final ExploradorRutas explorador = new ExploradorRutas();
    private final ImportadorCatalogo importadorJson = new ImportadorJson();
    private final ImportadorCatalogo importadorXml = new ImportadorXml();
    private final ExportadorCatalogo exportadorJson = new ExportadorJson();
    private final ExportadorCatalogo exportadorXml = new ExportadorXml();

    /** @param inventario colección común de productos de esta ejecución. */
    public MenuPrincipal(Inventario inventario) { this.inventario = inventario; }

    /** Ejecuta el menú principal hasta que el usuario selecciona salir. */
    public void iniciar() {
        int opcion;
        do {
            mostrarCabecera();
            System.out.println("1. Explorar ficheros y directorios");
            System.out.println("2. Importar productos desde JSON");
            System.out.println("3. Importar productos desde XML");
            System.out.println("4. Consultar productos");
            System.out.println("5. Exportar productos");
            System.out.println("6. Guardar y recuperar inventario");
            System.out.println("0. Salir");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 6);
            switch (opcion) {
                case 1 -> explorarRuta();
                case 2 -> importar("JSON", importadorJson);
                case 3 -> importar("XML", importadorXml);
                case 4 -> menuConsultas();
                case 5 -> menuExportaciones();
                case 6 -> menuSerializacion();
                case 0 -> System.out.println(ColoresConsola.pintar("Hasta pronto.", ColoresConsola.VERDE));
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void mostrarCabecera() {
        System.out.println(ColoresConsola.pintar("\n================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("         DATASTOCK - GESTIÓN DE INVENTARIO", ColoresConsola.NEGRITA));
        System.out.println(ColoresConsola.pintar("                 FASE 1 - UT1", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("Productos en memoria: " + inventario.tamano(), ColoresConsola.GRIS));
    }

    /** RF-FIC-01: pide la ruta y delega la exploración en ExploradorRutas. */
    private void explorarRuta() {
        System.out.println("\n===== EXPLORAR RUTA =====");
        String ruta = LectorConsola.leerTexto("Ruta: ");
        explorador.explorar(ruta);
        LectorConsola.pausar();
    }

    /**
     * RF-FIC-02 y RF-FIC-03: pide la ruta, delega la importación y muestra el resumen.
     * @param formato   texto para el título ("JSON" o "XML")
     * @param importador objeto que sabe leer ese formato
     */
    private void importar(String formato, ImportadorCatalogo importador) {
        System.out.println("\n===== IMPORTAR " + formato + " =====");
        String ruta = LectorConsola.leerTexto("Ruta: ");
        try {
            ResultadoImportacion resultado = importador.importar(ruta, inventario);
            mostrarResumen(resultado);
        } catch (ImportacionException ex) {
            System.out.println(ColoresConsola.pintar("Importación cancelada: " + ex.getMessage(), ColoresConsola.ROJO));
            System.out.println("No se ha añadido ningún producto al inventario.");
        }
        LectorConsola.pausar();
    }

    /** Muestra el resumen común de una importación (JSON o XML). */
    private void mostrarResumen(ResultadoImportacion resultado) {
        System.out.println("Productos encontrados: " + resultado.getEncontrados());
        System.out.println("Importados: " + resultado.getImportados() + " | Rechazados: " + resultado.getRechazados());
        if (resultado.getIncidencias().isEmpty()) {
            System.out.println(ColoresConsola.pintar("Importación finalizada.", ColoresConsola.VERDE));
        } else {
            System.out.println("INCIDENCIAS:");
            for (String incidencia : resultado.getIncidencias()) {
                System.out.println("  - " + incidencia);
            }
            System.out.println(ColoresConsola.pintar("Importación finalizada con incidencias.", ColoresConsola.AMARILLO));
        }
    }

    private void menuConsultas() {
        int opcion;
        do {
            System.out.println("\n---------- CONSULTA DE PRODUCTOS ----------");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 3);
            switch (opcion) {
                case 1 -> listarProductos();
                case 2 -> buscarPorCodigo();
                case 3 -> buscarPorNombre();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /** RF-FIC-04.1: listado completo ordenado por código. */
    private void listarProductos() {
        if (avisarSiVacio()) {
            return;
        }
        List<Producto> productos = inventario.listarOrdenado();
        mostrarTabla(productos);
        System.out.println("Total: " + productos.size() + " productos");
        LectorConsola.pausar();
    }

    /** RF-FIC-04.2: búsqueda por código exacto. */
    private void buscarPorCodigo() {
        if (avisarSiVacio()) {
            return;
        }
        String codigo = LectorConsola.leerTexto("Código: ");
        if (codigo.isBlank()) {
            System.out.println(ColoresConsola.pintar("Debes escribir un código.", ColoresConsola.AMARILLO));
        } else {
            inventario.buscarPorCodigo(codigo).ifPresentOrElse(
                    this::mostrarFicha,
                    () -> System.out.println(ColoresConsola.pintar(
                            "No existe ningún producto con el código " + codigo.trim() + ".", ColoresConsola.AMARILLO)));
        }
        LectorConsola.pausar();
    }

    /** RF-FIC-04.3: búsqueda por fragmento del nombre. */
    private void buscarPorNombre() {
        if (avisarSiVacio()) {
            return;
        }
        String texto = LectorConsola.leerTexto("Texto a buscar en el nombre: ");
        if (texto.isBlank()) {
            System.out.println(ColoresConsola.pintar("Debes escribir algún texto.", ColoresConsola.AMARILLO));
        } else {
            List<Producto> encontrados = inventario.buscarPorNombre(texto);
            if (encontrados.isEmpty()) {
                System.out.println(ColoresConsola.pintar(
                        "Ningún producto contiene «" + texto.trim() + "» en su nombre.", ColoresConsola.AMARILLO));
            } else {
                mostrarTabla(encontrados);
                System.out.println("Coincidencias: " + encontrados.size());
            }
        }
        LectorConsola.pausar();
    }

    /** Muestra un aviso si el inventario está vacío. @return true si lo estaba */
    private boolean avisarSiVacio() {
        if (!inventario.estaVacio()) {
            return false;
        }
        System.out.println(ColoresConsola.pintar(
                "El inventario está vacío. Importa un catálogo JSON o XML (opciones 2 y 3) o recupera un .dat (opción 6).",
                ColoresConsola.AMARILLO));
        LectorConsola.pausar();
        return true;
    }

    private void mostrarTabla(List<Producto> productos) {
        String formato = "%-12s %-32s %10s %6s %5s %5s%n";
        System.out.printf(formato, "CÓDIGO", "NOMBRE", "PRECIO", "STOCK", "CAT.", "PROV.");
        for (Producto p : productos) {
            System.out.printf(formato, p.getCodigo(), recortar(p.getNombre(), 32),
                    p.getPrecio().toPlainString() + " €", p.getStock(), p.getCategoriaId(), p.getProveedorId());
        }
    }

    private void mostrarFicha(Producto p) {
        System.out.println("Código: " + p.getCodigo());
        System.out.println("Nombre: " + p.getNombre());
        System.out.println("Precio: " + p.getPrecio().toPlainString() + " € | Stock: " + p.getStock());
        System.out.println("Categoría: " + p.getCategoriaId() + " | Proveedor: " + p.getProveedorId());
    }

    /** Acorta los nombres largos para que no descuadren la tabla. */
    private String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private void menuExportaciones() {
        int opcion;
        do {
            System.out.println("\n---------- EXPORTAR PRODUCTOS ----------");
            System.out.println("1. Exportar productos a JSON");
            System.out.println("2. Exportar productos a XML");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> exportar("JSON", exportadorJson);
                case 2 -> exportar("XML", exportadorXml);
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /**
     * RF-FIC-05: pide la ruta, confirma la sobrescritura y delega la exportación.
     * @param formato    texto para los mensajes ("JSON" o "XML")
     * @param exportador objeto que sabe escribir ese formato
     */
    private void exportar(String formato, ExportadorCatalogo exportador) {
        System.out.println("\n===== EXPORTAR " + formato + " =====");
        if (avisarSiVacio()) {
            return;
        }
        String texto = LectorConsola.leerTexto("Ruta de destino: ");
        try {
            Path destino = ComprobadorRutas.destinoEscribible(texto);
            if (!confirmarSobrescritura(destino)) {
                System.out.println("Exportación cancelada. El fichero existente no se ha modificado.");
            } else {
                List<Producto> productos = inventario.listarOrdenado();
                exportador.exportar(productos, destino);
                System.out.println(ColoresConsola.pintar("Fichero generado correctamente.", ColoresConsola.VERDE));
                System.out.println("Ruta: " + destino.toAbsolutePath());
                System.out.println("Formato: " + formato + " | Productos exportados: " + productos.size());
            }
        } catch (ExportacionException ex) {
            System.out.println(ColoresConsola.pintar("Exportación cancelada: " + ex.getMessage(), ColoresConsola.ROJO));
        }
        LectorConsola.pausar();
    }

    /** @return true si el fichero no existe o el usuario acepta sobrescribirlo */
    private boolean confirmarSobrescritura(Path destino) {
        return !Files.exists(destino)
                || LectorConsola.confirmar("El fichero " + destino.getFileName() + " ya existe. ¿Sobrescribirlo?");
    }

    private void menuSerializacion() {
        int opcion;
        do {
            System.out.println("\n---------- GUARDAR Y RECUPERAR INVENTARIO ----------");
            System.out.println("1. Guardar inventario en fichero .dat");
            System.out.println("2. Recuperar inventario desde fichero .dat");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> pendiente("RF-FIC-06 · Guardar inventario");
                case 2 -> pendiente("RF-FIC-06 · Recuperar inventario");
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    /** Sustituir las llamadas por servicios propios a medida que se implementen los RF. */
    private void pendiente(String requisito) {
        System.out.println(ColoresConsola.pintar("[PENDIENTE] " + requisito, ColoresConsola.AMARILLO));
        System.out.println("Esta funcionalidad debe desarrollarla el alumnado.");
        LectorConsola.pausar();
    }
}
