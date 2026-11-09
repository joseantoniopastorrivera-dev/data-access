package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import java.util.List;

/**
 * UT3 · Resultado que devuelve {@link LectorProductosJson} cuando el documento se ha podido leer.
 *
 * <p>Es un record: inmutable, con constructor, métodos de acceso, {@code equals} y
 * {@code toString} generados. El constructor guarda copias de solo lectura de las
 * listas, para que nadie pueda modificar el resultado desde fuera.</p>
 *
 * @param encontrados número de elementos que había en el array {@code "productos"}
 * @param productos   productos válidos, en el orden del fichero, listos para usarse
 * @param incidencias un motivo por cada producto rechazado, por ejemplo
 *                    {@code "Producto 1 (TEC-004): el precio debe ser mayor que 0"}
 */
public record ResultadoLecturaJson(int encontrados, List<Producto> productos, List<String> incidencias) {

    /** Copia defensiva: el resultado no cambia aunque se modifiquen las listas originales. */
    public ResultadoLecturaJson {
        productos = List.copyOf(productos);
        incidencias = List.copyOf(incidencias);
    }

    /**
     * Número de productos rechazados por datos inválidos.
     * Se calcula para que nunca pueda quedar descuadrado con los encontrados.
     * @return encontrados menos válidos
     */
    public int rechazados() {
        return encontrados - productos.size();
    }
}
