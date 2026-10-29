package es.iesisidra.datastock.ficheros;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Crea analizadores DOM protegidos frente a XXE (XML External Entity).
 * <p>Lo usan la importación XML (RF-FIC-03) y las consultas XPath (RF-FIC-07),
 * así que la configuración segura está en un solo sitio.</p>
 */
public final class XmlSeguro {
    private XmlSeguro() { }

    /**
     * Gestor de errores que no escribe nada en la consola: los errores se
     * lanzan como excepción y cada operación los explica a su manera.
     * Sin él, DOM imprime "[Fatal Error] ..." en System.err.
     */
    private static final ErrorHandler ERRORES_COMO_EXCEPCION = new ErrorHandler() {
        @Override public void warning(SAXParseException ex) { }
        @Override public void error(SAXParseException ex) throws SAXParseException { throw ex; }
        @Override public void fatalError(SAXParseException ex) throws SAXParseException { throw ex; }
    };

    /**
     * @return un DocumentBuilder que rechaza cualquier documento con DOCTYPE
     * @throws ParserConfigurationException si el analizador no admite alguna opción de seguridad
     */
    public static DocumentBuilder nuevoDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();

        // Protección principal: sin DOCTYPE no puede haber DTD ni entidades externas.
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        // Defensa en profundidad, por si la línea anterior se eliminase.
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);

        DocumentBuilder builder = fabrica.newDocumentBuilder();
        builder.setErrorHandler(ERRORES_COMO_EXCEPCION);
        return builder;
    }
}
