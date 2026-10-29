package es.iesisidra.datastock.ficheros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.validacion.DatosProducto;
import es.iesisidra.datastock.validacion.ValidadorProducto;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RF-FIC-02 · Importar un catálogo JSON con Gson.
 * <p>Lee el documento como árbol (JsonParser), traduce cada elemento a
 * DatosProducto, lo valida y añade al inventario solo los productos válidos
 * y no duplicados.</p>
 * <p>Se lee como árbol y no con {@code gson.fromJson(..., Clase.class)} porque
 * un solo producto con un tipo incorrecto haría fallar todo el documento, y el
 * enunciado exige importar los válidos y explicar cada rechazo.</p>
 */
public class ImportadorJson {
    private static final String CLAVE_PRODUCTOS = "productos";
    private static final Pattern POSICION_GSON = Pattern.compile("line (\\d+) column (\\d+)");

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * @param textoRuta  ruta del fichero escrita por el usuario
     * @param inventario colección común donde se añaden los productos válidos
     * @return resumen de la importación (el documento se pudo leer)
     * @throws ImportacionException si el documento entero no puede procesarse; el inventario no cambia
     */
    public ResultadoImportacion importar(String textoRuta, Inventario inventario) throws ImportacionException {
        Path ruta = comprobarRuta(textoRuta);
        JsonArray productos = leerArrayProductos(ruta);

        // A partir de aquí el documento es válido: cada producto se trata por separado.
        ResultadoImportacion resultado = new ResultadoImportacion();
        Set<String> codigosDelFichero = new HashSet<>();
        for (int i = 0; i < productos.size(); i++) {
            int posicion = i + 1;
            resultado.sumarEncontrado();
            JsonElement elemento = productos.get(i);
            if (!elemento.isJsonObject()) {
                resultado.anadirIncidencia("Producto " + posicion + ": no es un objeto JSON");
                continue;
            }
            DatosProducto datos = extraerDatos(posicion, elemento.getAsJsonObject());
            procesar(datos, inventario, codigosDelFichero, resultado);
        }
        return resultado;
    }

    /** Comprueba que la ruta existe, es un fichero regular y se puede leer. */
    private Path comprobarRuta(String textoRuta) throws ImportacionException {
        if (textoRuta == null || textoRuta.isBlank()) {
            throw new ImportacionException("Debes indicar la ruta del fichero.");
        }
        Path ruta;
        try {
            ruta = Path.of(textoRuta.trim());
        } catch (InvalidPathException ex) {
            throw new ImportacionException("La ruta contiene caracteres no válidos: " + ex.getMessage(), ex);
        }
        if (!Files.exists(ruta)) {
            throw new ImportacionException("El fichero no existe: " + ruta.toAbsolutePath());
        }
        if (!Files.isRegularFile(ruta)) {
            throw new ImportacionException("La ruta no es un fichero (¿es un directorio?): " + ruta.toAbsolutePath());
        }
        if (!Files.isReadable(ruta)) {
            throw new ImportacionException("No tienes permisos para leer el fichero: " + ruta.toAbsolutePath());
        }
        return ruta;
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
        } catch (JsonParseException | MalformedJsonException ex) {
            throw new ImportacionException("El fichero no es un JSON válido" + ubicacion(ex)
                    + ": revisa llaves, corchetes, comillas y comas.", ex);
        } catch (IOException ex) {
            throw new ImportacionException("No se pudo leer el fichero: " + ex.getMessage(), ex);
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

    /** Valida un producto, comprueba duplicados y lo añade o registra la incidencia. */
    private void procesar(DatosProducto datos, Inventario inventario,
                          Set<String> codigosDelFichero, ResultadoImportacion resultado) {
        List<String> errores = new ArrayList<>();
        Producto producto = validador.validar(datos, errores);

        if (producto != null && codigosDelFichero.contains(producto.getCodigo())) {
            errores.add("código duplicado dentro del mismo fichero");
        } else if (producto != null && !inventario.agregar(producto)) {
            errores.add("código duplicado: ya existe en el inventario");
        }

        if (errores.isEmpty()) {
            codigosDelFichero.add(producto.getCodigo());
            resultado.sumarImportado();
        } else {
            resultado.anadirIncidencia(identificar(datos) + ": " + String.join("; ", errores));
        }
    }

    /** "Producto 3 (CAB-005)", o solo "Producto 3" si no tiene código. */
    private String identificar(DatosProducto datos) {
        String codigo = ValidadorProducto.normalizarCodigo(datos.codigo());
        return "Producto " + datos.posicion() + (codigo == null ? "" : " (" + codigo + ")");
    }
}
