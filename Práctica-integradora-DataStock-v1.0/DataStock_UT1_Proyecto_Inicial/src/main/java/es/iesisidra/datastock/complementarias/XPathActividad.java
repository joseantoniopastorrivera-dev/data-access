package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import java.math.BigDecimal;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;

/**
 * RF-FIC-07 · Actividad independiente de consultas XPath.
 * <p>No forma parte del menú principal de DataStock. Abre un catálogo XML y
 * permite varias consultas sobre el documento ya abierto. La lógica XPath
 * está en {@link ConsultasXPath}; esta clase solo dialoga con el usuario.</p>
 * <p>Ejecución: {@code mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.XPathActividad}</p>
 */
public final class XPathActividad {
    private XPathActividad() { }

    public static void main(String[] args) {
        System.out.println("===== CONSULTAS XPATH =====");
        ConsultasXPath consultas = abrirFichero();
        if (consultas == null) {
            return;
        }
        int opcion;
        do {
            System.out.println("\n1. Por código | 2. Por categoría | 3. Precio superior | 4. Contar | 0. Salir");
            opcion = LectorConsola.leerOpcion("Opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> consultarCodigo(consultas);
                    case 2 -> consultarCategoria(consultas);
                    case 3 -> consultarPrecio(consultas);
                    case 4 -> System.out.println("Productos en el catálogo: " + consultas.contar());
                    case 0 -> System.out.println("Fin de las consultas.");
                    default -> throw new IllegalStateException("Opción inesperada");
                }
            } catch (XPathExpressionException ex) {
                System.out.println(ColoresConsola.pintar("Error al evaluar la consulta: " + ex.getMessage(), ColoresConsola.ROJO));
            }
        } while (opcion != 0);
    }

    /** Pide rutas hasta abrir un XML válido; una ruta vacía termina la actividad. */
    private static ConsultasXPath abrirFichero() {
        while (true) {
            String ruta = LectorConsola.leerTexto("Fichero XML (Intro para salir): ");
            if (ruta.isBlank()) {
                return null;
            }
            try {
                ConsultasXPath consultas = ConsultasXPath.abrir(ruta);
                System.out.println("Fichero abierto: " + ruta.trim());
                return consultas;
            } catch (ImportacionException ex) {
                System.out.println(ColoresConsola.pintar(ex.getMessage(), ColoresConsola.ROJO));
            }
        }
    }

    private static void consultarCodigo(ConsultasXPath consultas) throws XPathExpressionException {
        String codigo = LectorConsola.leerTexto("Código: ");
        if (codigo.isBlank()) {
            System.out.println(ColoresConsola.pintar("Debes escribir un código.", ColoresConsola.AMARILLO));
            return;
        }
        mostrar(consultas, consultas.porCodigo(codigo));
    }

    private static void consultarCategoria(ConsultasXPath consultas) throws XPathExpressionException {
        String texto = LectorConsola.leerTexto("Categoría: ").trim();
        int categoria;
        try {
            categoria = Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            categoria = 0;
        }
        if (categoria <= 0) {
            System.out.println(ColoresConsola.pintar("La categoría debe ser un entero positivo.", ColoresConsola.AMARILLO));
            return;
        }
        mostrar(consultas, consultas.porCategoria(categoria));
    }

    private static void consultarPrecio(ConsultasXPath consultas) throws XPathExpressionException {
        String texto = LectorConsola.leerTexto("Precio mínimo: ").trim();
        BigDecimal minimo;
        try {
            minimo = new BigDecimal(texto);
        } catch (NumberFormatException ex) {
            System.out.println(ColoresConsola.pintar("El precio debe ser un número (usa punto decimal).", ColoresConsola.AMARILLO));
            return;
        }
        if (minimo.signum() < 0) {
            System.out.println(ColoresConsola.pintar("El precio no puede ser negativo.", ColoresConsola.AMARILLO));
            return;
        }
        mostrar(consultas, consultas.precioSuperior(minimo));
    }

    private static void mostrar(ConsultasXPath consultas, List<Element> productos) throws XPathExpressionException {
        if (productos.isEmpty()) {
            System.out.println(ColoresConsola.pintar("No hay coincidencias.", ColoresConsola.AMARILLO));
            return;
        }
        for (Element p : productos) {
            System.out.printf("%-10s %-30s %10s € | categoría %s%n", consultas.texto(p, "codigo"),
                    consultas.texto(p, "nombre"), consultas.texto(p, "precio"), consultas.texto(p, "categoriaId"));
        }
        System.out.println("Productos encontrados: " + productos.size());
    }
}
