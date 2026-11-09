package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dao.EsquemaDAO;
import es.iesisidra.datastock.dto.InfoBaseDatos;
import es.iesisidra.datastock.jdbc.ErroresBD;
import es.iesisidra.datastock.servicio.DemoTransaccion;
import es.iesisidra.datastock.servicio.ImportadorMySQL;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import java.sql.SQLException;
import java.util.List;

/**
 * Opción 7 del menú principal: GESTIÓN MYSQL - UT2.
 * <p>Las opciones 2 a 7 se delegan en los submenús; aquí están la 1 (conexión),
 * la 8 (importar memoria → MySQL) y la 9 (demostración de transacciones).</p>
 */
public class MenuMySQL {
    private final Inventario inventario;
    private final MenuMaestros maestros = new MenuMaestros();
    private final MenuProductosBD productos = new MenuProductosBD();
    private final MenuStock stock = new MenuStock();
    private final EsquemaDAO esquemaDAO = new EsquemaDAO();
    private final ImportadorMySQL importador = new ImportadorMySQL();
    private final DemoTransaccion demo = new DemoTransaccion();

    /** @param inventario inventario en memoria de la UT1; solo lo lee la opción 8 */
    public MenuMySQL(Inventario inventario) { this.inventario = inventario; }

    public void iniciar() {
        int opcion;
        do {
            System.out.println(ColoresConsola.pintar("\n================================================", ColoresConsola.CIAN));
            System.out.println(ColoresConsola.pintar("               GESTIÓN MYSQL - UT2", ColoresConsola.NEGRITA));
            System.out.println(ColoresConsola.pintar("================================================", ColoresConsola.CIAN));
            System.out.println("1. Comprobar conexión y mostrar información de la BD");
            System.out.println("2. Gestionar categorías");
            System.out.println("3. Gestionar proveedores");
            System.out.println("4. Gestionar productos (CRUD)");
            System.out.println("5. Consultar y buscar productos");
            System.out.println("6. Registrar entrada o salida de stock");
            System.out.println("7. Consultar historial de movimientos");
            System.out.println("8. Importar productos de memoria a MySQL");
            System.out.println("9. Demostración de transacción con commit/rollback");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 9);
            switch (opcion) {
                case 1 -> conPausa(this::comprobarConexion);
                case 2 -> maestros.menuCategorias();
                case 3 -> maestros.menuProveedores();
                case 4 -> productos.menuProductos();
                case 5 -> productos.menuConsultas();
                case 6 -> stock.menuMovimientos();
                case 7 -> stock.menuHistorial();
                case 8 -> conPausa(this::importarMemoria);
                case 9 -> conPausa(this::demostrarTransacciones);
                default -> { }
            }
        } while (opcion != 0);
    }

    /** Operación que puede lanzar SQLException; así se captura en un único sitio. */
    @FunctionalInterface
    private interface OperacionBD { void ejecutar() throws SQLException; }

    private void conPausa(OperacionBD operacion) {
        try {
            operacion.ejecutar();
        } catch (SQLException ex) {
            Mensajes.error(ErroresBD.explicar(ex));
        }
        LectorConsola.pausar();
    }

    // ---------------------------------------------------------------------
    // 7.1 · RF-JDBC-01
    // ---------------------------------------------------------------------

    private void comprobarConexion() throws SQLException {
        Mensajes.titulo("COMPROBAR CONEXIÓN");
        System.out.println("Configuración leída de: " + ConexionBD.origenConfiguracion());
        InfoBaseDatos info = esquemaDAO.comprobar();
        Mensajes.ok("Conexión establecida correctamente.");
        System.out.println("Base de datos: " + info.baseDatos());
        System.out.println("Gestor: " + info.gestor());
        System.out.println("Versión: " + info.version());
        System.out.println("Driver JDBC: " + info.driver());
        if (info.tablasQueFaltan().isEmpty()) {
            Mensajes.ok("Están las cuatro tablas: categoria, proveedor, producto y movimiento_stock.");
        } else {
            Mensajes.error("Faltan tablas: " + String.join(", ", info.tablasQueFaltan())
                    + ". Ejecuta material-referencia/datastock-bd-mysql.sql.");
        }
        if (info.procedimiento()) {
            Mensajes.ok("Existe el procedimiento registrar_movimiento.");
        } else {
            Mensajes.error("Falta el procedimiento registrar_movimiento. Ejecuta el script SQL.");
        }
        if (info.productos() >= 0) {
            System.out.println("Productos almacenados: " + info.productos());
        }
        System.out.println("Conexión cerrada.");
    }

    // ---------------------------------------------------------------------
    // 7.8 · RF-JDBC-07
    // ---------------------------------------------------------------------

    private void importarMemoria() throws SQLException {
        Mensajes.titulo("IMPORTAR PRODUCTOS DE MEMORIA A MYSQL");
        System.out.println("Productos disponibles en memoria: " + inventario.tamano());
        if (inventario.estaVacio()) {
            Mensajes.aviso("No hay productos en memoria para importar. Usa antes las opciones 2, 3 o 6 del menú principal.");
            return;
        }
        if (!Mensajes.confirmar("¿Deseas importar estos productos a MySQL?")) {
            Mensajes.aviso("Importación cancelada. No se ha modificado nada.");
            return;
        }
        System.out.println("Iniciando comprobación de productos...");
        ImportadorMySQL.Resultado resultado = importador.importar(inventario.getProductos());

        for (ImportadorMySQL.Linea linea : resultado.lineas()) {
            System.out.println("Comprobando " + linea.codigo() + " - " + linea.nombre() + "...");
            switch (linea.estado()) {
                case VALIDO -> Mensajes.ok(linea.motivo());
                case DUPLICADO -> Mensajes.aviso(linea.motivo());
                case RECHAZADO -> Mensajes.error(linea.motivo());
                default -> { }
            }
        }
        Mensajes.titulo("RESULTADO DE LA IMPORTACIÓN");
        System.out.println("Productos analizados: " + resultado.lineas().size());
        System.out.println("Productos insertados: " + resultado.insertados());
        System.out.println("Omitidos por código duplicado: " + resultado.omitidos());
        System.out.println("Rechazados por datos incorrectos: " + resultado.rechazados());
        if (resultado.huboRollback()) {
            Mensajes.error("Error SQL durante el lote: " + resultado.errorLote());
            Mensajes.error("Se ha ejecutado rollback(): no se ha confirmado NINGUNA inserción de este lote.");
        } else if (resultado.insertados() > 0) {
            Mensajes.ok("Importación finalizada. Los productos válidos se han guardado en MySQL (commit).");
        } else {
            Mensajes.aviso("Importación finalizada. No había productos nuevos que insertar.");
        }
        System.out.println("El inventario en memoria no ha cambiado: sigue con " + inventario.tamano() + " productos.");
    }

    // ---------------------------------------------------------------------
    // 7.9 · RF-JDBC-08
    // ---------------------------------------------------------------------

    private void demostrarTransacciones() throws SQLException {
        Mensajes.titulo("DEMOSTRACIÓN DE TRANSACCIONES JDBC");
        System.out.println("Actividad técnica: no es una operación del inventario.");
        List<Integer> ids = demo.elegirIdsLibres(3);
        int idA1 = ids.get(0);
        int idA2 = ids.get(1);
        int idB = ids.get(2);
        System.out.println("IDs de categorías de prueba libres (rango reservado desde " + DemoTransaccion.PRIMER_ID_PRUEBA + "):");
        System.out.println("  Escenario A (commit):   " + idA1 + " y " + idA2 + " -> se insertarán y confirmarán.");
        System.out.println("  Escenario B (rollback): " + idB + " -> se insertará, se forzará un duplicado y se deshará.");
        if (!Mensajes.confirmar("¿Ejecutar la demostración?")) {
            Mensajes.aviso("Demostración cancelada. No se ha modificado nada.");
            return;
        }

        Mensajes.titulo("ESCENARIO A: COMMIT");
        DemoTransaccion.Escenario a = demo.escenarioCommit(idA1, idA2);
        a.pasos().forEach(paso -> System.out.println("  · " + paso));
        if (a.exito()) {
            Mensajes.ok("Commit correcto: las categorías " + idA1 + " y " + idA2 + " existen en MySQL.");
        } else {
            Mensajes.error("El escenario A no ha terminado como se esperaba.");
        }

        Mensajes.titulo("ESCENARIO B: ROLLBACK");
        DemoTransaccion.Escenario b = demo.escenarioRollback(idB);
        b.pasos().forEach(paso -> System.out.println("  · " + paso));
        if (b.errorEsperado() != null) {
            Mensajes.error("Error esperado: " + b.errorEsperado());
        }
        if (b.exito()) {
            Mensajes.ok("Rollback correcto: la categoría " + idB + " del intento fallido no se ha guardado.");
        } else {
            Mensajes.error("El escenario B no ha terminado como se esperaba.");
        }

        System.out.println();
        System.out.println("Limpieza manual (solo datos de demostración sin productos):");
        System.out.println("  DELETE FROM categoria WHERE id IN (" + idA1 + ", " + idA2 + ") AND nombre LIKE 'DEMO-TX %';");
        if (Mensajes.confirmar("¿Eliminar ahora las categorías de demostración " + idA1 + " y " + idA2 + "?")) {
            int borradas = demo.limpiar(List.of(idA1, idA2));
            Mensajes.ok("Categorías de demostración eliminadas: " + borradas + ".");
        } else {
            Mensajes.aviso("Se conservan las categorías " + idA1 + " y " + idA2 + " como evidencia del commit.");
        }
    }
}
