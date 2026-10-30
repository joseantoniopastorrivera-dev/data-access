package es.iesisidra.datastock.ficheros;

import java.io.IOException;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
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

    /**
     * Analiza un fichero XML con el analizador seguro y traduce cada fallo a un
     * mensaje claro. Lo comparten la importación XML y las consultas XPath.
     * @param ruta fichero ya comprobado con {@link ComprobadorRutas#ficheroLegible}
     * @param raizEsperada nombre que debe tener el elemento raíz
     * @return el documento DOM
     * @throws ImportacionException si tiene DOCTYPE, está mal formado, no se puede leer o la raíz no coincide
     */
    public static Document parsear(Path ruta, String raizEsperada) throws ImportacionException {
        Document documento;
        try {
            documento = nuevoDocumentBuilder().parse(ruta.toFile());
        } catch (SAXParseException ex) {
            if (String.valueOf(ex.getMessage()).contains("DOCTYPE")) {
                throw new ImportacionException("El XML contiene una declaración DOCTYPE (DTD o entidades); "
                        + "se rechaza por seguridad.", ex);
            }
            throw new ImportacionException("El fichero no es un XML bien formado (línea " + ex.getLineNumber()
                    + ", columna " + ex.getColumnNumber() + "): " + ex.getMessage(), ex);
        } catch (SAXException ex) {
            throw new ImportacionException("El fichero no es un XML válido: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ImportacionException("No se pudo leer el fichero: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            throw new ImportacionException("No se pudo configurar el analizador XML: " + ex.getMessage(), ex);
        }
        String raiz = documento.getDocumentElement().getNodeName();
        if (!raizEsperada.equals(raiz)) {
            throw new ImportacionException("La raíz del XML debe ser <" + raizEsperada + "> y es <" + raiz + ">.");
        }
        return documento;
    }
}
