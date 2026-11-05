package es.iesisidra.datastock.util;

/**
 * Códigos ANSI para personalizar la salida por consola.
 * <p>Se desactivan con la variable de entorno {@code NO_COLOR} o con la
 * propiedad de Java {@code -Ddatastock.color=false}. En terminales que no
 * admiten ANSI puede aparecer texto sin formato o secuencias de escape.</p>
 */
public final class ColoresConsola {
    private ColoresConsola() { }
    public static final String RESET = "\u001B[0m";
    public static final String NEGRITA = "\u001B[1m";
    public static final String ROJO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String AZUL = "\u001B[34m";
    public static final String CIAN = "\u001B[36m";
    public static final String GRIS = "\u001B[90m";

    /** @return true si está habilitado el coloreado ANSI. */
    public static boolean habilitados() {
        return System.getenv("NO_COLOR") == null
            && !"false".equalsIgnoreCase(System.getProperty("datastock.color", "true"));
    }
    /** Devuelve texto coloreado, o texto plano si se desactiva ANSI. */
    public static String pintar(String texto, String color) {
        return habilitados() ? color + texto + RESET : texto;
    }
}
