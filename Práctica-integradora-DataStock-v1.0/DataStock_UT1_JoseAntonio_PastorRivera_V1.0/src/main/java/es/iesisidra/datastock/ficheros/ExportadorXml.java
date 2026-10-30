package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * RF-FIC-05 · Exportar el inventario a XML mediante DOM.
 * <p>Construye en memoria el árbol catalogo/producto/campos y lo guarda con
 * un Transformer, en UTF-8 y con sangría. Es el mismo formato que se importa.</p>
 */
public class ExportadorXml implements ExportadorCatalogo {
    /**
     * La cabecera se escribe a mano: el Transformer del JDK la pondría en la
     * misma línea que &lt;catalogo&gt;, y además añadiría standalone="no".
     */
    private static final String CABECERA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.lineSeparator();

    @Override
    public void exportar(List<Producto> productos, Path destino) throws ExportacionException {
        Document documento;
        Transformer transformer;
        try {
            documento = construirDocumento(productos);
            transformer = crearTransformer();
        } catch (ParserConfigurationException | TransformerConfigurationException ex) {
            throw new ExportacionException("No se pudo preparar el documento XML: " + ex.getMessage(), ex);
        }
        EscrituraAtomica.escribir(destino, temporal -> {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                salida.write(CABECERA.getBytes(StandardCharsets.UTF_8));
                transformer.transform(new DOMSource(documento), new StreamResult(salida));
            }
        });
    }

    private Document construirDocumento(List<Producto> productos) throws ParserConfigurationException {
        Document documento = XmlSeguro.nuevoDocumentBuilder().newDocument();
        Element catalogo = documento.createElement("catalogo");
        documento.appendChild(catalogo);
        for (Producto p : productos) {
            Element producto = documento.createElement("producto");
            anadirCampo(documento, producto, "codigo", p.getCodigo());
            anadirCampo(documento, producto, "nombre", p.getNombre());
            anadirCampo(documento, producto, "precio", p.getPrecio().toPlainString());
            anadirCampo(documento, producto, "stock", String.valueOf(p.getStock()));
            anadirCampo(documento, producto, "categoriaId", String.valueOf(p.getCategoriaId()));
            anadirCampo(documento, producto, "proveedorId", String.valueOf(p.getProveedorId()));
            catalogo.appendChild(producto);
        }
        return documento;
    }

    /** Crea &lt;campo&gt;valor&lt;/campo&gt; dentro del producto. setTextContent escapa &amp;, &lt; y &gt;. */
    private void anadirCampo(Document documento, Element producto, String campo, String valor) {
        Element elemento = documento.createElement(campo);
        elemento.setTextContent(valor);
        producto.appendChild(elemento);
    }

    private Transformer crearTransformer() throws TransformerConfigurationException {
        TransformerFactory fabrica = TransformerFactory.newInstance();
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = fabrica.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return transformer;
    }
}
