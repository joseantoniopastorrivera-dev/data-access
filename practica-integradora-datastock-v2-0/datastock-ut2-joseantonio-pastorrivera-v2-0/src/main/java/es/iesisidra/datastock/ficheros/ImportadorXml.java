package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.validacion.DatosProducto;
import java.nio.file.Path;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * RF-FIC-03 · Importar un catálogo XML mediante DOM.
 * <p>Equivalente a {@link ImportadorJson}: solo cambia cómo se lee el documento.
 * La validación, los duplicados y el resumen los hace {@link ProcesoImportacion}.</p>
 */
public class ImportadorXml implements ImportadorCatalogo {
    private static final String RAIZ = "catalogo";
    private static final String ELEMENTO_PRODUCTO = "producto";

    /**
     * @param textoRuta  ruta del fichero escrita por el usuario
     * @param inventario colección común donde se añaden los productos válidos
     * @return resumen de la importación (el documento se pudo leer)
     * @throws ImportacionException si el documento entero no puede procesarse; el inventario no cambia
     */
    @Override
    public ResultadoImportacion importar(String textoRuta, Inventario inventario) throws ImportacionException {
        Path ruta = ComprobadorRutas.ficheroLegible(textoRuta);
        Element raiz = XmlSeguro.parsear(ruta, RAIZ).getDocumentElement();

        // A partir de aquí el documento es válido: cada producto se trata por separado.
        ProcesoImportacion proceso = new ProcesoImportacion(inventario);
        int posicion = 0;
        NodeList hijos = raiz.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() != Node.ELEMENT_NODE) {
                continue;   // saltos de línea, espacios y comentarios entre etiquetas
            }
            posicion++;
            if (ELEMENTO_PRODUCTO.equals(hijo.getNodeName())) {
                proceso.procesar(extraerDatos(posicion, (Element) hijo));
            } else {
                proceso.rechazar(posicion, "elemento <" + hijo.getNodeName() + "> inesperado; se esperaba <producto>");
            }
        }
        return proceso.getResultado();
    }

    /** Traduce un elemento &lt;producto&gt; a DatosProducto (todos los campos como texto). */
    private DatosProducto extraerDatos(int posicion, Element producto) {
        return new DatosProducto(posicion,
                texto(producto, "codigo"), texto(producto, "nombre"), texto(producto, "precio"),
                texto(producto, "stock"), texto(producto, "categoriaId"), texto(producto, "proveedorId"));
    }

    /**
     * Devuelve el texto del primer hijo directo con ese nombre, o null si no existe.
     * No se usa getElementsByTagName porque busca en toda la profundidad del árbol.
     */
    private String texto(Element producto, String campo) {
        NodeList hijos = producto.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.ELEMENT_NODE && campo.equals(hijo.getNodeName())) {
                return hijo.getTextContent();
            }
        }
        return null;
    }
}
