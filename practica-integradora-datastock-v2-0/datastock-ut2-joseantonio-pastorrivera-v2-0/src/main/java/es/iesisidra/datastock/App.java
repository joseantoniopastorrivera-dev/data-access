package es.iesisidra.datastock;

import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.ui.MenuPrincipal;

/**
 * Punto de entrada de DataStock (UT1 + UT2).
 * <p>Se crea un único inventario en memoria y se comparte con el menú. Las
 * conexiones a MySQL no se abren al arrancar: cada operación de la opción 7
 * abre la suya y la cierra al terminar.</p>
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
