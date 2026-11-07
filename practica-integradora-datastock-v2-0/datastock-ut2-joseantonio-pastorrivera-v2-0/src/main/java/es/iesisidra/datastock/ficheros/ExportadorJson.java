package es.iesisidra.datastock.ficheros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import es.iesisidra.datastock.modelo.Producto;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * RF-FIC-05 · Exportar el inventario a JSON con Gson.
 * <p>Genera el mismo formato que se importa: un objeto raíz con el array
 * "productos", de modo que el fichero exportado puede volver a importarse.</p>
 */
public class ExportadorJson implements ExportadorCatalogo {

    /** Con sangría para que sea legible y sin convertir caracteres como &lt; o &amp; en <. */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public void exportar(List<Producto> productos, Path destino) throws ExportacionException {
        JsonObject raiz = construirDocumento(productos);
        EscrituraAtomica.escribir(destino, temporal -> {
            try (BufferedWriter escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                gson.toJson(raiz, escritor);
            }
        });
    }

    /**
     * Construye el árbol JSON campo a campo. Se hace a mano, y no con
     * gson.toJson(productos), para que los nombres y el orden de las claves
     * no dependan de cómo se llamen los atributos privados de Producto.
     */
    private JsonObject construirDocumento(List<Producto> productos) {
        JsonArray array = new JsonArray();
        for (Producto p : productos) {
            JsonObject objeto = new JsonObject();
            objeto.addProperty("codigo", p.getCodigo());
            objeto.addProperty("nombre", p.getNombre());
            objeto.addProperty("precio", p.getPrecio());   // BigDecimal: se escribe sin perder decimales
            objeto.addProperty("stock", p.getStock());
            objeto.addProperty("categoriaId", p.getCategoriaId());
            objeto.addProperty("proveedorId", p.getProveedorId());
            array.add(objeto);
        }
        JsonObject raiz = new JsonObject();
        raiz.add("productos", array);
        return raiz;
    }
}
