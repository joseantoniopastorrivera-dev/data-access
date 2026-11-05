package es.iesisidra.datastock;

import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.ui.MenuPrincipal;

/**
 * Punto de entrada de DataStock, fase UT1.
 * <p>Se crea un único inventario en memoria y se comparte con el menú.
 * No se realizan conexiones a MySQL en esta fase.</p>
 */
public final class App {
    private App() { }
    /** Inicia el programa de consola. */
    public static void main(String[] args) {
        Inventario inventario = new Inventario();
        MenuPrincipal menu = new MenuPrincipal(inventario);
        menu.iniciar();
    }
}
