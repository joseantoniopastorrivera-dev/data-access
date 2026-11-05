package es.iesisidra.datastock.modelo;

import java.time.LocalDateTime;

/**
 * Registro histórico de entrada o salida de unidades de un producto.
 * <p>Se facilita como parte del modelo global. Los movimientos y las
 * transacciones se implementarán en UT2, no en esta primera entrega.</p>
 * <p>{@code stockAnterior} indica las unidades antes de la operación y
 * {@code stockPosterior} las disponibles inmediatamente después.</p>
 */
public class MovimientoStock {
    private long id;
    private String productoCodigo;
    private TipoMovimiento tipo;
    private int cantidad;
    private LocalDateTime fecha;
    private int stockAnterior;
    private int stockPosterior;

    /** Tipos de movimientos admitidos por el modelo de DataStock. */
    public enum TipoMovimiento { ENTRADA, SALIDA }

    /** Constructor sin argumentos. */
    public MovimientoStock() { }
    /** Construye un movimiento con los campos del modelo relacional. */
    public MovimientoStock(long id, String productoCodigo, TipoMovimiento tipo, int cantidad,
                           LocalDateTime fecha, int stockAnterior, int stockPosterior) {
        this.id = id; this.productoCodigo = productoCodigo; this.tipo = tipo;
        this.cantidad = cantidad; this.fecha = fecha;
        this.stockAnterior = stockAnterior; this.stockPosterior = stockPosterior;
    }
    /** @return identificador del movimiento. */
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    /** @return código del producto afectado. */
    public String getProductoCodigo() { return productoCodigo; }
    public void setProductoCodigo(String productoCodigo) { this.productoCodigo = productoCodigo; }
    /** @return ENTRADA o SALIDA. */
    public TipoMovimiento getTipo() { return tipo; }
    public void setTipo(TipoMovimiento tipo) { this.tipo = tipo; }
    /** @return número positivo de unidades del movimiento. */
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    /** @return instante en que se registra el movimiento. */
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    /** @return unidades anteriores a la operación. */
    public int getStockAnterior() { return stockAnterior; }
    public void setStockAnterior(int stockAnterior) { this.stockAnterior = stockAnterior; }
    /** @return unidades posteriores a la operación. */
    public int getStockPosterior() { return stockPosterior; }
    public void setStockPosterior(int stockPosterior) { this.stockPosterior = stockPosterior; }
    @Override public String toString() {
        return "MovimientoStock{id=" + id + ", productoCodigo='" + productoCodigo + "', tipo=" + tipo
            + ", cantidad=" + cantidad + ", fecha=" + fecha + ", stockAnterior=" + stockAnterior
            + ", stockPosterior=" + stockPosterior + "}";
    }
}
