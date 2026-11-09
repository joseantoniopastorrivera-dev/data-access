package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.MovimientoDAO;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.dto.ProductoDetalle;
import es.iesisidra.datastock.jdbc.ErroresBD;
import es.iesisidra.datastock.modelo.MovimientoStock;
import es.iesisidra.datastock.modelo.MovimientoStock.TipoMovimiento;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.validacion.ReglasCampos;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Submenús 7.6 (movimientos, RF-JDBC-05) y 7.7 (historial, RF-JDBC-06).
 */
class MenuStock {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();

    // =====================================================================
    // 7.6 MOVIMIENTOS
    // =====================================================================

    void menuMovimientos() {
        int opcion;
        do {
            Mensajes.titulo("MENÚ DE MOVIMIENTOS");
            System.out.println("1. Registrar una ENTRADA de stock");
            System.out.println("2. Registrar una SALIDA de stock");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            try {
                switch (opcion) {
                    case 1 -> registrar(TipoMovimiento.ENTRADA);
                    case 2 -> registrar(TipoMovimiento.SALIDA);
                    default -> { }
                }
            } catch (SQLException ex) {
                Mensajes.error(ErroresBD.explicar(ex));
            }
            if (opcion != 0) {
                LectorConsola.pausar();
            }
        } while (opcion != 0);
    }

    private void registrar(TipoMovimiento tipo) throws SQLException {
        Mensajes.titulo("REGISTRAR " + tipo);
        Optional<String> codigo = Formulario.pedir("Código de producto (Intro = cancelar): ", Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        Optional<ProductoDetalle> producto = productoDAO.buscarPorCodigo(codigo.get());
        if (producto.isEmpty()) {
            Mensajes.error("No existe ningún producto con el código " + codigo.get() + ".");
            return;
        }
        System.out.println("Producto: " + producto.get().nombre() + " | Stock actual: " + producto.get().stock());
        Optional<Integer> cantidad = Formulario.pedir("Cantidad (entero mayor que 0): ",
                texto -> ReglasCampos.enteroPositivo(texto, "la cantidad"));
        if (cantidad.isEmpty()) {
            return;
        }
        if (!Mensajes.confirmar("¿Registrar " + tipo + " de " + cantidad.get() + " unidades de " + codigo.get() + "?")) {
            Mensajes.aviso("Movimiento cancelado. No se ha modificado nada.");
            return;
        }
        // La comprobación de stock suficiente NO se hace aquí: la hace el procedimiento,
        // con la fila bloqueada, que es el único sitio donde el dato no puede cambiar a mitad.
        try {
            MovimientoStock m = movimientoDAO.registrar(codigo.get(), tipo, cantidad.get());
            Mensajes.ok("Movimiento " + m.getId() + " registrado: " + m.getTipo() + " de " + m.getCantidad()
                    + " unidades. Stock anterior " + m.getStockAnterior() + " -> posterior " + m.getStockPosterior() + ".");
        } catch (SQLException ex) {
            int stock = productoDAO.buscarPorCodigo(codigo.get()).map(ProductoDetalle::stock).orElse(-1);
            Mensajes.error(ErroresBD.explicar(ex) + ". El stock permanece en " + stock + " y no se ha creado ningún movimiento.");
        }
    }

    // =====================================================================
    // 7.7 HISTORIAL (solo lectura)
    // =====================================================================

    void menuHistorial() {
        int opcion;
        do {
            Mensajes.titulo("MENÚ DE HISTORIAL");
            System.out.println("1. Consultar todos los movimientos");
            System.out.println("2. Consultar movimientos de un producto");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            try {
                switch (opcion) {
                    case 1 -> {
                        Mensajes.titulo("HISTORIAL COMPLETO");
                        mostrarTabla(movimientoDAO.listarTodos(), "No hay movimientos registrados.");
                    }
                    case 2 -> historialProducto();
                    default -> { }
                }
            } catch (SQLException ex) {
                Mensajes.error(ErroresBD.explicar(ex));
            }
            if (opcion != 0) {
                LectorConsola.pausar();
            }
        } while (opcion != 0);
    }

    private void historialProducto() throws SQLException {
        Mensajes.titulo("HISTORIAL DE UN PRODUCTO");
        Optional<String> codigo = Formulario.pedir("Código de producto (Intro = cancelar): ", Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        // Se distingue "no existe el producto" de "existe pero no tiene movimientos".
        Optional<ProductoDetalle> producto = productoDAO.buscarPorCodigo(codigo.get());
        if (producto.isEmpty()) {
            Mensajes.error("No existe ningún producto con el código " + codigo.get() + ".");
            return;
        }
        System.out.println("Producto: " + producto.get().nombre() + " | Stock actual: " + producto.get().stock());
        mostrarTabla(movimientoDAO.listarPorProducto(codigo.get()), "Sin movimientos registrados.");
    }

    private static void mostrarTabla(List<MovimientoStock> movimientos, String siVacio) {
        if (movimientos.isEmpty()) {
            Mensajes.aviso(siVacio);
            return;
        }
        String formato = "%-6s %-19s %-12s %-8s %8s %9s %10s%n";
        System.out.printf(formato, "ID", "FECHA", "PRODUCTO", "TIPO", "CANTIDAD", "ANTERIOR", "POSTERIOR");
        for (MovimientoStock m : movimientos) {
            System.out.printf(formato, m.getId(), m.getFecha().format(FECHA), m.getProductoCodigo(), m.getTipo(),
                    m.getCantidad(), m.getStockAnterior(), m.getStockPosterior());
        }
        System.out.println("Total de movimientos: " + movimientos.size());
    }
}
