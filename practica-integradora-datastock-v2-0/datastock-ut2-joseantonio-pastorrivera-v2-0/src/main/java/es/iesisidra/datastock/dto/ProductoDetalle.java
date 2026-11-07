package es.iesisidra.datastock.dto;

import java.math.BigDecimal;

/**
 * DTO (objeto de transferencia de datos) con una fila de la consulta de productos
 * con JOIN: los datos del producto más los NOMBRES de su categoría y proveedor.
 * <p>No se reutiliza {@code Producto} porque ese modelo solo guarda los IDs; este
 * objeto existe para presentar el resultado de una consulta (RF-JDBC-04.4: construir
 * objetos a partir del ResultSet en vez de mostrar valores sueltos sin identificar).</p>
 * <p>Es un record: inmutable, con constructor, getters, equals y toString generados.</p>
 */
public record ProductoDetalle(String codigo, String nombre, BigDecimal precio, int stock,
                              int categoriaId, String categoriaNombre,
                              int proveedorId, String proveedorNombre) { }
