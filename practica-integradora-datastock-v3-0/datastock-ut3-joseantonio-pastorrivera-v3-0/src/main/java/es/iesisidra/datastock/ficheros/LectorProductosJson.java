package es.iesisidra.datastock.ficheros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.validacion.DatosProducto;
import es.iesisidra.datastock.validacion.ValidadorProducto;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UT3 · Componente reutilizable que lee un catálogo de productos en formato JSON.
 *
 * <p>Recibe la ruta de un fichero con la estructura {@code {"productos": [ {...}, ... ]}},
 * lo lee con Gson en modo estricto, valida cada producto con las reglas de la UT1
 * ({@link ValidadorProducto}) y devuelve los productos válidos como objetos
 * {@link Producto} dentro de un {@link ResultadoLecturaJson}.</p>
 *
 * <p>Responsabilidad única: <b>leer y validar</b>. El componente:</p>
 * <ul>
 *   <li>no imprime nada ni lee del teclado (no depende de la consola),</li>
 *   <li>no modifica el inventario en memoria,</li>
 *   <li>no inserta nada en MySQL.</li>
 * </ul>
 * <p>Por eso se puede reutilizar desde el menú de DataStock, desde otra aplicación
 * o desde una prueba JUnit. Quien lo llama decide qué hacer con los productos.</p>
 *
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * LectorProductosJson lector = new LectorProductosJson();
 * ResultadoLecturaJson resultado = lector.leer(Path.of("datos/ejemplos/catalogo_valido.json"));
 * for (Producto p : resultado.productos()) { ... }
 * }</pre>
 *
 * <p>La lógica de lectura procede de {@code ImportadorJson} (UT1, RF-FIC-02) y se ha
 * trasladado sin cambiar las reglas de validación.</p>
 */
public class LectorProductosJson {
    private static final String CLAVE_PRODUCTOS = "productos";
    /** Gson da la posición de los errores en inglés: "line 4 column 7". */
    private static final Pattern POSICION_GSON = Pattern.compile("line (\\d+) column (\\d+)");

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * Lee el fichero JSON y devuelve los productos válidos.
     *
     * <p>Los errores se tratan en dos niveles:</p>
     * <ul>
     *   <li><b>Errores de documento</b> (no se puede leer el fichero entero): se lanza
     *       {@link ImportacionException} y no se devuelve ningún producto.</li>
     *   <li><b>Datos inválidos de un producto</b> (precio negativo, campo que falta, código
     *       repetido dentro del fichero...): ese producto se descarta, el motivo se añade a
     *       {@link ResultadoLecturaJson#incidencias()} y la lectura continúa con los demás.</li>
     * </ul>
     *
     * @param ruta ruta del fichero JSON que se quiere leer
     * @return resultado con el número de productos encontrados, los productos válidos
     *         (en el orden del fichero) y los motivos de rechazo de los inválidos
     * @throws ImportacionException si la ruta es nula, el fichero no existe, no es un fichero
     *         regular o no se puede leer, no está en UTF-8, no es un JSON válido, tiene contenido
     *         después del objeto raíz, está vacío o no contiene el array {@code "productos"}
     */
    public ResultadoLecturaJson leer(Path ruta) throws ImportacionException {
        if (ruta == null) {
            throw new ImportacionException("Debes indicar la ruta del fichero.");
        }
        ComprobadorRutas.ficheroLegible(ruta);
        JsonArray elementos = leerArrayProductos(ruta);

        // A partir de aquí el documento es correcto: cada producto se valida por separado.
        List<Producto> validos = new ArrayList<>();
        List<String> incidencias = new ArrayList<>();
        Set<String> codigosDelFichero = new HashSet<>();
        for (int i = 0; i < elementos.size(); i++) {
            int posicion = i + 1;
            JsonElement elemento = elementos.get(i);
            if (!elemento.isJsonObject()) {
                incidencias.add("Producto " + posicion + ": no es un objeto JSON");
                continue;
            }
            DatosProducto datos = extraerDatos(posicion, elemento.getAsJsonObject());
            List<String> errores = new ArrayList<>();
            Producto producto = validador.validar(datos, errores);
            // Set.add devuelve false si el código ya estaba: segundo producto con el mismo código.
            if (producto != null && !codigosDelFichero.add(producto.getCodigo())) {
                errores.add("código duplicado dentro del mismo fichero");
            }
            if (errores.isEmpty()) {
                validos.add(producto);
            } else {
                incidencias.add(identificar(datos) + ": " + String.join("; ", errores));
            }
        }
        return new ResultadoLecturaJson(elementos.size(), validos, incidencias);
    }

    /** Lee el JSON en modo estricto y devuelve el array "productos" de la raíz. */
    private JsonArray leerArrayProductos(Path ruta) throws ImportacionException {
        JsonElement raiz;
        try (BufferedReader lector = Files.newBufferedReader(ruta, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(lector);
            jsonReader.setStrictness(Strictness.STRICT);
            raiz = JsonParser.parseReader(jsonReader);
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new ImportacionException("El JSON tiene contenido después del objeto raíz.");
            }
        } catch (JsonIOException ex) {
            // Gson envuelve aquí los errores de lectura del Reader, como una codificación incorrecta.
            throw errorDeLectura(ex.getCause() instanceof IOException io ? io : new IOException(ex));
        } catch (JsonParseException | MalformedJsonException ex) {
            throw new ImportacionException("El fichero no es un JSON válido" + ubicacion(ex)
                    + ": revisa llaves, corchetes, comillas y comas.", ex);
        } catch (IOException ex) {
            throw errorDeLectura(ex);
        }

        if (raiz.isJsonNull()) {
            throw new ImportacionException("El fichero está vacío.");
        }
        if (!raiz.isJsonObject()) {
            throw new ImportacionException("La raíz del JSON debe ser un objeto con la clave \"" + CLAVE_PRODUCTOS + "\".");
        }
        JsonElement productos = raiz.getAsJsonObject().get(CLAVE_PRODUCTOS);
        if (productos == null || !productos.isJsonArray()) {
            throw new ImportacionException("El JSON no contiene el array \"" + CLAVE_PRODUCTOS + "\".");
        }
        return productos.getAsJsonArray();
    }

    /**
     * Traduce un error de lectura a un mensaje claro. Si el fichero no está en
     * UTF-8 (por ejemplo, guardado en Latin-1 con una «ñ»), el decodificador lanza
     * CharacterCodingException, que no es un error de sintaxis JSON.
     */
    private ImportacionException errorDeLectura(IOException ex) {
        if (ex instanceof CharacterCodingException) {
            return new ImportacionException("El fichero no está codificado en UTF-8: guárdalo como UTF-8 "
                    + "y vuelve a intentarlo.", ex);
        }
        return new ImportacionException("No se pudo leer el fichero: " + ex.getMessage(), ex);
    }

    /** Extrae "línea X, columna Y" del mensaje de Gson, que viene en inglés. */
    private String ubicacion(Exception ex) {
        Matcher m = POSICION_GSON.matcher(String.valueOf(ex.getMessage()));
        return m.find() ? " (línea " + m.group(1) + ", columna " + m.group(2) + ")" : "";
    }

    /** Traduce un objeto JSON a DatosProducto (todos los campos como texto). */
    private DatosProducto extraerDatos(int posicion, JsonObject objeto) {
        return new DatosProducto(posicion,
                texto(objeto, "codigo"), texto(objeto, "nombre"), texto(objeto, "precio"),
                texto(objeto, "stock"), texto(objeto, "categoriaId"), texto(objeto, "proveedorId"));
    }

    /**
     * Devuelve el valor del campo como texto, o null si falta, vale null o no es
     * un valor simple. No se usa getAsInt(): con 1.5 devuelve 1 sin avisar.
     */
    private String texto(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || !valor.isJsonPrimitive()) {
            return null;
        }
        return valor.getAsString();
    }

    /** "Producto 3 (CAB-005)", o solo "Producto 3" si no tiene código. */
    private String identificar(DatosProducto datos) {
        String codigo = ValidadorProducto.normalizarCodigo(datos.codigo());
        return "Producto " + datos.posicion() + (codigo == null ? "" : " (" + codigo + ")");
    }
}
