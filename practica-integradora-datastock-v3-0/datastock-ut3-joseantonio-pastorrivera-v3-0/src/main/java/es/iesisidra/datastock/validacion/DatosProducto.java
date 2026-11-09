package es.iesisidra.datastock.validacion;

/**
 * Valores de un producto tal y como se leen del fichero, todavía como texto
 * y sin validar. Un campo ausente o nulo en el fichero llega como {@code null}.
 * <p>JSON y XML se traducen a este mismo registro, de modo que ambos formatos
 * comparten exactamente las mismas reglas de validación (RF-FIC-02 y 03).</p>
 *
 * @param posicion número del producto dentro del fichero, empezando en 1 (para los mensajes)
 */
public record DatosProducto(int posicion, String codigo, String nombre, String precio,
                            String stock, String categoriaId, String proveedorId) { }
