package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.ComprobadorRutas;
import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.ficheros.XmlSeguro;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * RF-FIC-07 · Consultas XPath sobre un catálogo XML abierto.
 * <p>Trabaja directamente sobre el documento DOM, sin importar los productos
 * al inventario. Los valores del usuario nunca se concatenan en la expresión:
 * categoría y precio se pasan como variables XPath ($categoria, $precio) y el
 * código se compara en Java.</p>
 */
public class ConsultasXPath {
    private final Document documento;
    private final XPath xpath;
    private final Map<String, Object> variables = new HashMap<>();

    private ConsultasXPath(Document documento) {
        this.documento = documento;
        this.xpath = XPathFactory.newInstance().newXPath();
        // Las expresiones leen $nombre de este mapa en el momento de evaluarse.
        this.xpath.setXPathVariableResolver((QName nombre) -> variables.get(nombre.getLocalPart()));
    }

    /**
     * Abre el XML con el analizador seguro (rechaza DOCTYPE) y comprueba la raíz.
     * @throws ImportacionException si el fichero no existe, está mal formado, tiene DTD o la raíz no es catalogo
     */
    public static ConsultasXPath abrir(String textoRuta) throws ImportacionException {
        Path ruta = ComprobadorRutas.ficheroLegible(textoRuta);
        return new ConsultasXPath(XmlSeguro.parsear(ruta, "catalogo"));
    }

    /** Consulta 1 · Productos cuyo código coincide, sin distinguir mayúsculas ni espacios extremos. */
    public List<Element> porCodigo(String codigo) throws XPathExpressionException {
        String buscado = codigo.trim();
        List<Element> resultado = new ArrayList<>();
        for (Element producto : nodos("/catalogo/producto")) {
            if (buscado.equalsIgnoreCase(texto(producto, "codigo"))) {
                resultado.add(producto);
            }
        }
        return resultado;
    }

    /** Consulta 2 · Productos de una categoría. */
    public List<Element> porCategoria(int categoria) throws XPathExpressionException {
        variables.put("categoria", categoria);
        return nodos("/catalogo/producto[categoriaId = $categoria]");
    }

    /** Consulta 3 · Productos con precio estrictamente mayor que el indicado (el propio valor queda excluido). */
    public List<Element> precioSuperior(BigDecimal minimo) throws XPathExpressionException {
        variables.put("precio", minimo.doubleValue());
        return nodos("/catalogo/producto[precio > $precio]");
    }

    /** Consulta 4 · Número de elementos producto, aunque alguno tenga datos incorrectos. */
    public int contar() throws XPathExpressionException {
        Double total = (Double) xpath.evaluate("count(/catalogo/producto)", documento, XPathConstants.NUMBER);
        return total.intValue();
    }

    /** Texto de un hijo del producto, sin espacios extremos; cadena vacía si no existe. */
    public String texto(Element producto, String campo) throws XPathExpressionException {
        return xpath.evaluate(campo, producto).trim();
    }

    private List<Element> nodos(String expresion) throws XPathExpressionException {
        NodeList lista = (NodeList) xpath.evaluate(expresion, documento, XPathConstants.NODESET);
        List<Element> elementos = new ArrayList<>();
        for (int i = 0; i < lista.getLength(); i++) {
            elementos.add((Element) lista.item(i));
        }
        return elementos;
    }
}
