package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.dao.CategoriaDAO;
import es.iesisidra.datastock.dao.ProveedorDAO;
import es.iesisidra.datastock.jdbc.ErroresBD;
import es.iesisidra.datastock.modelo.Categoria;
import es.iesisidra.datastock.modelo.Proveedor;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.validacion.ReglasCampos;
import es.iesisidra.datastock.validacion.ReglasCampos.Campo;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Submenús 7.2 (categorías) y 7.3 (proveedores) · RF-JDBC-02.
 * <p>Esta clase solo dialoga con el usuario; el SQL está en los DAO. Cada operación
 * captura su SQLException, la explica en rojo y vuelve al submenú.</p>
 */
class MenuMaestros {
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    // =====================================================================
    // 7.2 CATEGORÍAS
    // =====================================================================

    void menuCategorias() {
        int opcion;
        do {
            Mensajes.titulo("MENÚ DE CATEGORÍAS");
            System.out.println("1. Dar de alta una categoría");
            System.out.println("2. Listar categorías");
            System.out.println("3. Modificar el nombre de una categoría");
            System.out.println("4. Eliminar una categoría");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> altaCategoria();
                    case 2 -> listarCategorias();
                    case 3 -> modificarCategoria();
                    case 4 -> eliminarCategoria();
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

    private void altaCategoria() throws SQLException {
        Mensajes.titulo("ALTA DE CATEGORÍA");
        Optional<Integer> id = Formulario.pedir("ID de categoría (Intro = cancelar): ", Formulario::idPositivo);
        if (id.isEmpty()) {
            return;
        }
        if (categoriaDAO.buscarPorId(id.get()).isPresent()) {
            Mensajes.error("Ya existe una categoría con el ID " + id.get() + ".");
            return;
        }
        Optional<String> nombre = Formulario.pedir("Nombre (1-60 caracteres): ", nombreCategoria());
        if (nombre.isEmpty()) {
            return;
        }
        if (categoriaDAO.nombreEnUso(nombre.get(), 0)) {
            Mensajes.error("Ya existe otra categoría llamada «" + nombre.get() + "».");
            return;
        }
        try {
            categoriaDAO.insertar(new Categoria(id.get(), nombre.get()));
            Mensajes.ok("Categoría registrada correctamente.");
        } catch (SQLException ex) {
            // Última barrera: si alguien la creó entre la comprobación y el INSERT, salta la PK/UNIQUE.
            Mensajes.error(ErroresBD.esDuplicado(ex) ? "La categoría ya existe (ID o nombre duplicado)." : ErroresBD.explicar(ex));
        }
    }

    private void listarCategorias() throws SQLException {
        Mensajes.titulo("LISTADO DE CATEGORÍAS");
        List<Categoria> categorias = categoriaDAO.listar();
        if (categorias.isEmpty()) {
            Mensajes.aviso("No hay categorías registradas.");
            return;
        }
        System.out.printf("%-6s %s%n", "ID", "NOMBRE");
        Mensajes.separador();
        for (Categoria c : categorias) {
            System.out.printf("%-6d %s%n", c.getId(), c.getNombre());
        }
        System.out.println("Total: " + categorias.size());
    }

    private void modificarCategoria() throws SQLException {
        Mensajes.titulo("MODIFICAR CATEGORÍA");
        Optional<Integer> id = Formulario.pedir("ID de categoría (Intro = cancelar): ", Formulario::idPositivo);
        if (id.isEmpty()) {
            return;
        }
        Optional<Categoria> actual = categoriaDAO.buscarPorId(id.get());
        if (actual.isEmpty()) {
            Mensajes.error("No existe ninguna categoría con el ID " + id.get() + ".");
            return;
        }
        System.out.println("Nombre actual: " + actual.get().getNombre());
        Optional<String> nombre = Formulario.pedir("Nuevo nombre (1-60 caracteres, Intro = cancelar): ", nombreCategoria());
        if (nombre.isEmpty()) {
            return;
        }
        if (categoriaDAO.nombreEnUso(nombre.get(), id.get())) {
            Mensajes.error("Ya existe otra categoría llamada «" + nombre.get() + "».");
            return;
        }
        if (!Mensajes.confirmar("¿Cambiar «" + actual.get().getNombre() + "» por «" + nombre.get() + "»?")) {
            Mensajes.aviso("Modificación cancelada. No se ha cambiado nada.");
            return;
        }
        int filas = categoriaDAO.actualizarNombre(id.get(), nombre.get());
        if (filas == 1) {
            Mensajes.ok("Categoría " + id.get() + " actualizada.");
        } else {
            Mensajes.error("No se ha modificado ninguna fila: la categoría ya no existe.");
        }
    }

    private void eliminarCategoria() throws SQLException {
        Mensajes.titulo("ELIMINAR CATEGORÍA");
        Optional<Integer> id = Formulario.pedir("ID de categoría (Intro = cancelar): ", Formulario::idPositivo);
        if (id.isEmpty()) {
            return;
        }
        Optional<Categoria> actual = categoriaDAO.buscarPorId(id.get());
        if (actual.isEmpty()) {
            Mensajes.error("No existe ninguna categoría con el ID " + id.get() + ".");
            return;
        }
        System.out.println("ID: " + actual.get().getId() + " | Nombre: " + actual.get().getNombre());
        if (!Mensajes.confirmar("¿Confirmas la eliminación?")) {
            Mensajes.aviso("Eliminación cancelada. La categoría se conserva.");
            return;
        }
        try {
            int filas = categoriaDAO.eliminar(id.get());
            if (filas == 1) {
                Mensajes.ok("Categoría " + id.get() + " eliminada.");
            } else {
                Mensajes.error("No se ha borrado ninguna fila: la categoría ya no existe.");
            }
        } catch (SQLException ex) {
            if (ErroresBD.esClaveForanea(ex)) {
                Mensajes.error("No se puede eliminar la categoría " + id.get()
                        + ": tiene productos asociados (integridad referencial). No se ha borrado nada.");
            } else {
                Mensajes.error(ErroresBD.explicar(ex));
            }
        }
    }

    private static java.util.function.Function<String, Campo<String>> nombreCategoria() {
        return Formulario.texto("el nombre", 1, ReglasCampos.NOMBRE_CATEGORIA_MAX);
    }

    // =====================================================================
    // 7.3 PROVEEDORES
    // =====================================================================

    void menuProveedores() {
        int opcion;
        do {
            Mensajes.titulo("MENÚ DE PROVEEDORES");
            System.out.println("1. Dar de alta un proveedor");
            System.out.println("2. Listar proveedores");
            System.out.println("3. Modificar un proveedor");
            System.out.println("4. Eliminar un proveedor");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> altaProveedor();
                    case 2 -> listarProveedores();
                    case 3 -> modificarProveedor();
                    case 4 -> eliminarProveedor();
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

    private void altaProveedor() throws SQLException {
        Mensajes.titulo("ALTA DE PROVEEDOR");
        Optional<Integer> id = Formulario.pedir("ID de proveedor (Intro = cancelar): ", Formulario::idPositivo);
        if (id.isEmpty()) {
            return;
        }
        if (proveedorDAO.buscarPorId(id.get()).isPresent()) {
            Mensajes.error("Ya existe un proveedor con el ID " + id.get() + ".");
            return;
        }
        Optional<String> nombre = Formulario.pedir("Nombre (máximo 100 caracteres): ", nombreProveedor());
        if (nombre.isEmpty()) {
            return;
        }
        String email = pedirEmailAlta();
        if (email != null && proveedorDAO.emailEnUso(email, 0)) {
            Mensajes.error("El correo " + email + " ya está registrado en otro proveedor.");
            return;
        }
        try {
            proveedorDAO.insertar(new Proveedor(id.get(), nombre.get(), email));
            Mensajes.ok("Proveedor registrado correctamente.");
        } catch (SQLException ex) {
            Mensajes.error(ErroresBD.esDuplicado(ex) ? "El proveedor ya existe (ID o correo duplicado)." : ErroresBD.explicar(ex));
        }
    }

    /** @return el correo validado, o null si el usuario pulsa Intro (se guardará NULL) */
    private String pedirEmailAlta() {
        while (true) {
            String texto = LectorConsola.leerTexto("Email (opcional, Intro = sin correo): ").trim();
            if (texto.isEmpty()) {
                return null;
            }
            String error = ReglasCampos.errorEmail(texto);
            if (error == null) {
                return texto;
            }
            Mensajes.error(Mensajes.frase(error));
        }
    }

    private void listarProveedores() throws SQLException {
        Mensajes.titulo("LISTADO DE PROVEEDORES");
        List<Proveedor> proveedores = proveedorDAO.listar();
        if (proveedores.isEmpty()) {
            Mensajes.aviso("No hay proveedores registrados.");
            return;
        }
        String formato = "%-6s %-32s %s%n";
        System.out.printf(formato, "ID", "NOMBRE", "EMAIL");
        Mensajes.separador();
        for (Proveedor p : proveedores) {
            System.out.printf(formato, p.getId(), p.getNombre(), p.getEmail() == null ? "—" : p.getEmail());
        }
        System.out.println("Total: " + proveedores.size());
    }

    private void modificarProveedor() throws SQLException {
        Mensajes.titulo("MODIFICAR PROVEEDOR");
        Optional<Integer> id = Formulario.pedir("ID de proveedor (Intro = cancelar): ", Formulario::idPositivo);
        if (id.isEmpty()) {
            return;
        }
        Optional<Proveedor> encontrado = proveedorDAO.buscarPorId(id.get());
        if (encontrado.isEmpty()) {
            Mensajes.error("No existe ningún proveedor con el ID " + id.get() + ".");
            return;
        }
        Proveedor actual = encontrado.get();
        System.out.println("Nombre actual: " + actual.getNombre());
        System.out.println("Email actual: " + (actual.getEmail() == null ? "—" : actual.getEmail()));
        System.out.println("Pulsa Intro para conservar cada dato. En el email, escribe - para dejarlo sin valor.");

        String nombre = Formulario.pedirOConservar("Nombre", actual.getNombre(), nombreProveedor());
        String email = pedirEmailModificacion(actual.getEmail());
        if (email != null && proveedorDAO.emailEnUso(email, id.get())) {
            Mensajes.error("El correo " + email + " ya está registrado en otro proveedor.");
            return;
        }
        if (!Mensajes.confirmar("¿Guardar los cambios del proveedor " + id.get() + "?")) {
            Mensajes.aviso("Modificación cancelada. No se ha cambiado nada.");
            return;
        }
        try {
            int filas = proveedorDAO.actualizar(new Proveedor(id.get(), nombre, email));
            if (filas == 1) {
                Mensajes.ok("Proveedor " + id.get() + " actualizado.");
            } else {
                Mensajes.error("No se ha modificado ninguna fila: el proveedor ya no existe.");
            }
        } catch (SQLException ex) {
            Mensajes.error(ErroresBD.esDuplicado(ex) ? "Ese correo ya pertenece a otro proveedor." : ErroresBD.explicar(ex));
        }
    }

    /** Intro conserva el actual, "-" lo deja en NULL y cualquier otro texto se valida. */
    private String pedirEmailModificacion(String actual) {
        while (true) {
            String texto = LectorConsola.leerTexto("Email [" + (actual == null ? "—" : actual)
                    + "] (Intro = conservar, - = sin correo): ").trim();
            if (texto.isEmpty()) {
                return actual;
            }
            if (texto.equals("-")) {
                return null;
            }
            String error = ReglasCampos.errorEmail(texto);
            if (error == null) {
                return texto;
            }
            Mensajes.error(Mensajes.frase(error));
        }
    }

    private void eliminarProveedor() throws SQLException {
        Mensajes.titulo("ELIMINAR PROVEEDOR");
        Optional<Integer> id = Formulario.pedir("ID de proveedor (Intro = cancelar): ", Formulario::idPositivo);
        if (id.isEmpty()) {
            return;
        }
        Optional<Proveedor> actual = proveedorDAO.buscarPorId(id.get());
        if (actual.isEmpty()) {
            Mensajes.error("No existe ningún proveedor con el ID " + id.get() + ".");
            return;
        }
        Proveedor p = actual.get();
        System.out.println("ID: " + p.getId() + " | Nombre: " + p.getNombre() + " | Email: "
                + (p.getEmail() == null ? "—" : p.getEmail()));
        if (!Mensajes.confirmar("¿Confirmas la eliminación?")) {
            Mensajes.aviso("Eliminación cancelada. El proveedor se conserva.");
            return;
        }
        try {
            int filas = proveedorDAO.eliminar(id.get());
            if (filas == 1) {
                Mensajes.ok("Proveedor " + id.get() + " eliminado.");
            } else {
                Mensajes.error("No se ha borrado ninguna fila: el proveedor ya no existe.");
            }
        } catch (SQLException ex) {
            if (ErroresBD.esClaveForanea(ex)) {
                Mensajes.error("No se puede eliminar el proveedor " + id.get()
                        + ": hay productos que lo referencian (integridad referencial). No se ha borrado nada.");
            } else {
                Mensajes.error(ErroresBD.explicar(ex));
            }
        }
    }

    private static java.util.function.Function<String, Campo<String>> nombreProveedor() {
        return Formulario.texto("el nombre", 1, ReglasCampos.NOMBRE_PROVEEDOR_MAX);
    }
}
