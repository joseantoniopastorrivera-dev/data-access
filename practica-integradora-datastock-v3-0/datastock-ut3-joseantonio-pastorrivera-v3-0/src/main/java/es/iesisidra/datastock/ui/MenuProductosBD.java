package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.dao.ProductoDAO;
import es.iesisidra.datastock.dao.ProveedorDAO;
import es.iesisidra.datastock.dto.ProductoDetalle;
import es.iesisidra.datastock.jdbc.ConsultaProductoJdbc;
import es.iesisidra.datastock.jdbc.ErroresBD;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.validacion.ReglasCampos;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Submenús 7.4 (CRUD de productos, RF-JDBC-03) y 7.5 (consultas, RF-JDBC-04).
 * <p>Todo trabaja directamente sobre MySQL: el inventario en memoria de la UT1 no se usa aquí.</p>
 */
class MenuProductosBD {
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    /** UT3: componente reutilizable de consulta por código (opción 7.5.2). */
    private final ConsultaProductoJdbc consultaProducto = new ConsultaProductoJdbc();

    // =====================================================================
    // 7.4 MANTENIMIENTO DE PRODUCTOS
    // =====================================================================

    void menuProductos() {
        int opcion;
        do {
            Mensajes.titulo("MENÚ DE PRODUCTOS");
            System.out.println("1. Dar de alta un producto");
            System.out.println("2. Consultar producto por código");
            System.out.println("3. Modificar un producto");
            System.out.println("4. Eliminar un producto");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> alta();
                    case 2 -> consultarPorCodigo();
                    case 3 -> modificar();
                    case 4 -> eliminar();
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

    private void alta() throws SQLException {
        Mensajes.titulo("ALTA DE PRODUCTO");
        Optional<String> codigo = Formulario.pedir("Código (1-12: A-Z, 0-9 y -; Intro = cancelar): ",
                Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        if (productoDAO.existe(codigo.get())) {
            Mensajes.error("El producto " + codigo.get() + " ya existe en MySQL. No se sobrescribe.");
            return;
        }
        Optional<String> nombre = Formulario.pedir("Nombre (3-100 caracteres): ",
                Formulario.texto("el nombre", ReglasCampos.NOMBRE_PRODUCTO_MIN, ReglasCampos.NOMBRE_PRODUCTO_MAX));
        if (nombre.isEmpty()) {
            return;
        }
        Optional<BigDecimal> precio = Formulario.pedir("Precio (mayor que 0, máximo 2 decimales, ej. 49.90): ",
                ReglasCampos::precio);
        if (precio.isEmpty()) {
            return;
        }
        Optional<Integer> stock = Formulario.pedir("Stock inicial (entero, 0 o más): ",
                texto -> ReglasCampos.enteroNoNegativo(texto, "el stock"));
        if (stock.isEmpty()) {
            return;
        }
        Optional<Integer> categoriaId = Formulario.pedir("ID de categoría: ", Formulario::idPositivo);
        if (categoriaId.isEmpty() || !existeCategoria(categoriaId.get())) {
            return;
        }
        Optional<Integer> proveedorId = Formulario.pedir("ID de proveedor: ", Formulario::idPositivo);
        if (proveedorId.isEmpty() || !existeProveedor(proveedorId.get())) {
            return;
        }
        try {
            productoDAO.insertar(new Producto(codigo.get(), nombre.get(), precio.get(), stock.get(),
                    categoriaId.get(), proveedorId.get()));
            Mensajes.ok("Producto " + codigo.get() + " guardado correctamente.");
        } catch (SQLException ex) {
            Mensajes.error(ErroresBD.esDuplicado(ex) ? "El producto " + codigo.get() + " ya existe." : ErroresBD.explicar(ex));
        }
    }

    private void consultarPorCodigo() throws SQLException {
        Mensajes.titulo("CONSULTAR PRODUCTO");
        Optional<String> codigo = Formulario.pedir("Código (Intro = cancelar): ", Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        productoDAO.buscarPorCodigo(codigo.get()).ifPresentOrElse(
                MenuProductosBD::mostrarFicha,
                () -> Mensajes.aviso("No existe ningún producto con el código " + codigo.get() + " en MySQL."));
    }

    private void modificar() throws SQLException {
        Mensajes.titulo("MODIFICAR PRODUCTO");
        Optional<String> codigo = Formulario.pedir("Código (Intro = cancelar): ", Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        Optional<ProductoDetalle> encontrado = productoDAO.buscarPorCodigo(codigo.get());
        if (encontrado.isEmpty()) {
            Mensajes.error("No existe ningún producto con el código " + codigo.get() + ".");
            return;
        }
        ProductoDetalle actual = encontrado.get();
        mostrarFicha(actual);
        System.out.println("Pulsa Intro para conservar cada valor. El código y el stock no se modifican aquí:");
        System.out.println("el stock solo cambia con el menú 6 (entradas y salidas), para que quede en el historial.");

        String nombre = Formulario.pedirOConservar("Nombre", actual.nombre(),
                Formulario.texto("el nombre", ReglasCampos.NOMBRE_PRODUCTO_MIN, ReglasCampos.NOMBRE_PRODUCTO_MAX));
        BigDecimal precio = Formulario.pedirOConservar("Precio", actual.precio(), ReglasCampos::precio);
        int categoriaId = Formulario.pedirOConservar("ID de categoría", actual.categoriaId(), Formulario::idPositivo);
        if (!existeCategoria(categoriaId)) {
            return;
        }
        int proveedorId = Formulario.pedirOConservar("ID de proveedor", actual.proveedorId(), Formulario::idPositivo);
        if (!existeProveedor(proveedorId)) {
            return;
        }
        if (!Mensajes.confirmar("¿Guardar los cambios de " + codigo.get() + "?")) {
            Mensajes.aviso("Modificación cancelada. No se ha cambiado nada.");
            return;
        }
        // El stock que se pasa no se usa: el UPDATE no incluye esa columna.
        int filas = productoDAO.actualizar(new Producto(codigo.get(), nombre, precio, actual.stock(), categoriaId, proveedorId));
        if (filas == 1) {
            Mensajes.ok("Producto " + codigo.get() + " actualizado.");
        } else {
            Mensajes.error("No se ha modificado ninguna fila: el producto ya no existe.");
        }
    }

    private void eliminar() throws SQLException {
        Mensajes.titulo("ELIMINAR PRODUCTO");
        Optional<String> codigo = Formulario.pedir("Código (Intro = cancelar): ", Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        Optional<ProductoDetalle> actual = productoDAO.buscarPorCodigo(codigo.get());
        if (actual.isEmpty()) {
            Mensajes.error("No existe ningún producto con el código " + codigo.get() + ".");
            return;
        }
        mostrarFicha(actual.get());
        if (!Mensajes.confirmar("¿Confirmas la eliminación?")) {
            Mensajes.aviso("Eliminación cancelada. El producto se conserva.");
            return;
        }
        try {
            int filas = productoDAO.eliminar(codigo.get());
            if (filas == 1) {
                Mensajes.ok("Producto " + codigo.get() + " eliminado.");
            } else {
                Mensajes.error("No se ha borrado ninguna fila: el producto ya no existe.");
            }
        } catch (SQLException ex) {
            if (ErroresBD.esClaveForanea(ex)) {
                Mensajes.error("No se puede eliminar " + codigo.get()
                        + ": tiene movimientos de stock en el historial. Se conserva el producto y su historial.");
            } else {
                Mensajes.error(ErroresBD.explicar(ex));
            }
        }
    }

    /** Comprueba la clave foránea antes de escribir y explica el problema si falla. */
    private boolean existeCategoria(int id) throws SQLException {
        if (categoriaDAO.buscarPorId(id).isPresent()) {
            return true;
        }
        Mensajes.error("No existe la categoría con ID " + id
                + ". Dala de alta antes en el menú de categorías (integridad referencial). No se ha guardado nada.");
        return false;
    }

    private boolean existeProveedor(int id) throws SQLException {
        if (proveedorDAO.buscarPorId(id).isPresent()) {
            return true;
        }
        Mensajes.error("No existe el proveedor con ID " + id
                + ". Dalo de alta antes en el menú de proveedores (integridad referencial). No se ha guardado nada.");
        return false;
    }

    // =====================================================================
    // 7.5 CONSULTAS
    // =====================================================================

    void menuConsultas() {
        int opcion;
        do {
            Mensajes.titulo("MENÚ DE CONSULTAS");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("4. Consultar productos con stock inferior a un umbral");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> {
                        Mensajes.titulo("PRODUCTOS ALMACENADOS EN MYSQL");
                        mostrarTabla(productoDAO.listarTodos(), "No hay productos almacenados en MySQL.");
                    }
                    case 2 -> buscarPorCodigo();
                    case 3 -> buscarPorNombre();
                    case 4 -> stockBajo();
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

    /**
     * 7.5.2 · Buscar producto por código, integrada en la UT3 con {@link ConsultaProductoJdbc}.
     * <p>El menú pide y valida el código, llama al componente y presenta el resultado:
     * [OK] y la ficha si existe, [AVISO] si no existe. Si MySQL falla, la SQLException
     * llega a {@link #menuConsultas()}, que muestra [ERROR] con {@link ErroresBD}.</p>
     */
    private void buscarPorCodigo() throws SQLException {
        Mensajes.titulo("BUSCAR PRODUCTO POR CÓDIGO");
        Optional<String> codigo = Formulario.pedir("Código (Intro = cancelar): ", Formulario::codigoProducto);
        if (codigo.isEmpty()) {
            return;
        }
        Optional<ProductoDetalle> producto = consultaProducto.buscarPorCodigo(codigo.get());
        if (producto.isPresent()) {
            Mensajes.ok("Producto encontrado en MySQL.");
            mostrarFicha(producto.get());
        } else {
            Mensajes.aviso("No existe ningún producto con el código " + codigo.get() + " en MySQL.");
        }
    }

    private void buscarPorNombre() throws SQLException {
        Mensajes.titulo("BUSCAR POR NOMBRE");
        Optional<String> texto = Formulario.pedir("Texto a buscar en el nombre (Intro = cancelar): ",
                Formulario.texto("el texto", 1, ReglasCampos.NOMBRE_PRODUCTO_MAX));
        if (texto.isPresent()) {
            mostrarTabla(productoDAO.buscarPorNombre(texto.get()),
                    "Ningún producto contiene «" + texto.get() + "» en su nombre.");
        }
    }

    private void stockBajo() throws SQLException {
        Mensajes.titulo("STOCK INFERIOR A UN UMBRAL");
        Optional<Integer> umbral = Formulario.pedir("Umbral (entero, 0 o más; Intro = cancelar): ",
                texto -> ReglasCampos.enteroNoNegativo(texto, "el umbral"));
        if (umbral.isPresent()) {
            mostrarTabla(productoDAO.stockInferiorA(umbral.get()),
                    "No hay productos con stock inferior a " + umbral.get() + ".");
        }
    }

    private static void mostrarTabla(List<ProductoDetalle> productos, String siVacio) {
        if (productos.isEmpty()) {
            Mensajes.aviso(siVacio);
            return;
        }
        String formato = "%-12s %-26s %11s %6s %-16s %-20s%n";
        System.out.printf(formato, "CÓDIGO", "NOMBRE", "PRECIO", "STOCK", "CATEGORÍA", "PROVEEDOR");
        for (ProductoDetalle p : productos) {
            System.out.printf(formato, p.codigo(), recortar(p.nombre(), 26), p.precio().toPlainString() + " €",
                    p.stock(), recortar(p.categoriaNombre(), 16), recortar(p.proveedorNombre(), 20));
        }
        System.out.println("Total de productos encontrados: " + productos.size());
    }

    static void mostrarFicha(ProductoDetalle p) {
        System.out.println("Código: " + p.codigo());
        System.out.println("Nombre: " + p.nombre());
        System.out.println("Precio: " + p.precio().toPlainString() + " €");
        System.out.println("Stock: " + p.stock());
        System.out.println("Categoría: " + p.categoriaId() + " - " + p.categoriaNombre());
        System.out.println("Proveedor: " + p.proveedorId() + " - " + p.proveedorNombre());
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
