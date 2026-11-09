package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;

/**
 * Convenciones de salida de la UT2 (apartado 5.1): [OK] en verde, [ERROR] en rojo
 * y [AVISO] en amarillo. El prefijo hace que el mensaje se entienda aunque la
 * terminal no muestre colores ANSI; {@link ColoresConsola#pintar} añade RESET al final.
 */
final class Mensajes {
    private static final String LINEA = "-".repeat(48);

    private Mensajes() { }

    static void ok(String texto) { System.out.println(ColoresConsola.pintar("[OK] " + texto, ColoresConsola.VERDE)); }

    static void error(String texto) { System.out.println(ColoresConsola.pintar("[ERROR] " + texto, ColoresConsola.ROJO)); }

    static void aviso(String texto) { System.out.println(ColoresConsola.pintar("[AVISO] " + texto, ColoresConsola.AMARILLO)); }

    /** Pide confirmación S/N con el prefijo de aviso en amarillo. */
    static boolean confirmar(String pregunta) {
        return LectorConsola.confirmar(ColoresConsola.pintar("[AVISO] " + pregunta, ColoresConsola.AMARILLO));
    }

    static void titulo(String texto) {
        System.out.println();
        System.out.println(ColoresConsola.pintar(texto, ColoresConsola.NEGRITA));
        System.out.println(LINEA);
    }

    static void separador() { System.out.println(LINEA); }

    /** Pone en mayúscula la primera letra de los mensajes de ReglasCampos ("el precio..." → "El precio..."). */
    static String frase(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1) + ".";
    }
}
