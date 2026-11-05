package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Estado común del inventario durante la ejecución de UT1.
 * <p>Es la única clase que modifica la colección: las importaciones añaden
 * con {@link #agregar}, la recuperación de un .dat sustituye con
 * {@link #sustituirTodo} y las consultas reciben copias o vistas de solo lectura.</p>
 */
public class Inventario {
    private static final Comparator<Producto> POR_CODIGO = Comparator.comparing(Producto::getCodigo);

    private final List<Producto> productos = new ArrayList<>();

    /**
     * Obtiene la colección que comparten las opciones del menú.
     * @return vista de solo lectura: para modificarla hay que usar los métodos de esta clase
     */
    public List<Producto> getProductos() { return Collections.unmodifiableList(productos); }

    /** @return número de productos en memoria */
    public int tamano() { return productos.size(); }

    /** @return true si no hay productos en memoria */
    public boolean estaVacio() { return productos.isEmpty(); }

    /**
     * Indica si ya existe un producto con ese código, sin distinguir mayúsculas
     * y sin tener en cuenta los espacios extremos.
     */
    public boolean existeCodigo(String codigo) {
        return buscarPorCodigo(codigo).isPresent();
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

    /**
     * RF-FIC-06 · Sustituye todo el inventario por los productos recuperados.
     * <p>A diferencia de importar (que añade), recuperar un .dat reemplaza la colección.
     * Solo debe llamarse cuando la recuperación completa ha ido bien.</p>
     */
    public void sustituirTodo(List<Producto> nuevos) {
        productos.clear();
        productos.addAll(nuevos);
    }

    /**
     * RF-FIC-04 · Todos los productos ordenados por código.
     * @return una copia ordenada; la colección original no se reordena ni se modifica
     */
    public List<Producto> listarOrdenado() {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(POR_CODIGO);
        return copia;
    }

    /**
     * RF-FIC-04 · Busca un código exacto, ignorando mayúsculas y espacios extremos.
     * @return el producto, o un Optional vacío si no existe o el código está en blanco
     */
    public Optional<Producto> buscarPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String buscado = codigo.trim();
        return productos.stream()
                .filter(p -> p.getCodigo().equalsIgnoreCase(buscado))
                .findFirst();
    }

    /**
     * RF-FIC-04 · Productos cuyo nombre contiene el texto, sin distinguir mayúsculas.
     * @return lista ordenada por código (vacía si no hay coincidencias o el texto está en blanco)
     */
    public List<Producto> buscarPorNombre(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        return productos.stream()
                .filter(p -> p.getNombre().toLowerCase(Locale.ROOT).contains(buscado))
                .sorted(POR_CODIGO)
                .toList();
    }
}
