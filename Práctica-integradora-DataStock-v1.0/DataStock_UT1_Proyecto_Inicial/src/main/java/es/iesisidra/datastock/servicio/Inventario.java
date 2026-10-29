package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado común del inventario durante la ejecución de UT1.
 * <p>Esta clase solo proporciona la colección compartida. No implementa
 * importación, validación, búsqueda, exportación ni persistencia: esos son
 * ejercicios que deberá desarrollar el alumnado.</p>
 */
public class Inventario {
    private final List<Producto> productos = new ArrayList<>();

    /**
     * Obtiene la colección que comparten las opciones del menú.
     * <p>ATENCIÓN: la lista es modificable. El alumnado deberá aplicar las
     * reglas del enunciado antes de añadir o sustituir elementos.</p>
     */
    public List<Producto> getProductos() { return productos; }

    /**
     * Indica si ya existe un producto con ese código, sin distinguir mayúsculas
     * y sin tener en cuenta los espacios extremos.
     */
    public boolean existeCodigo(String codigo) {
        if (codigo == null) {
            return false;
        }
        String buscado = codigo.trim();
        return productos.stream().anyMatch(p -> p.getCodigo().equalsIgnoreCase(buscado));
    }

    /**
     * Añade un producto ya validado. Rechaza los duplicados como última barrera.
     * @return true si se añadió; false si el código ya existía
     */
    public boolean agregar(Producto producto) {
        if (producto == null || existeCodigo(producto.getCodigo())) {
            return false;
        }
        productos.add(producto);
        return true;
    }
}
