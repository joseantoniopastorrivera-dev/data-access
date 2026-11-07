package es.iesisidra.datastock.validacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de cada campo, compartidas por UT1 (importar ficheros) y UT2 (consola y JDBC).
 * <p>Reproducen en Java las restricciones del script MySQL (apartado 3.1), para
 * rechazar los datos incorrectos ANTES de ejecutar la sentencia SQL. La base de
 * datos sigue siendo la última barrera: si algo se escapa, su CHECK, UNIQUE o
 * FOREIGN KEY lo rechazará y la aplicación capturará la excepción.</p>
 * <p>Los métodos {@code error...} devuelven el motivo del rechazo o {@code null}
 * si el valor es correcto. Los que además convierten el texto devuelven un {@link Campo}.</p>
 */
public final class ReglasCampos {
    public static final int CODIGO_MAX = 12;
    public static final int NOMBRE_PRODUCTO_MIN = 3;
    public static final int NOMBRE_PRODUCTO_MAX = 100;
    public static final int NOMBRE_CATEGORIA_MAX = 60;
    public static final int NOMBRE_PROVEEDOR_MAX = 100;
    public static final int EMAIL_MAX = 120;
    /** DECIMAL(10,2): 10 cifras en total, 2 decimales, luego 8 enteras. */
    private static final int PRECIO_MAX_ENTEROS = 8;
    private static final int PRECIO_DECIMALES = 2;

    private static final Pattern PATRON_CODIGO = Pattern.compile("[A-Z0-9-]+");
    /** Formato básico: algo@algo.algo, sin espacios. No pretende cubrir todo el RFC 5322. */
    private static final Pattern PATRON_EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private ReglasCampos() { }

    /**
     * Resultado de convertir un texto: el valor o el motivo del error (uno de los dos).
     * @param <T> tipo del valor convertido
     */
    public record Campo<T>(T valor, String error) {
        public static <T> Campo<T> ok(T valor) { return new Campo<>(valor, null); }
        public static <T> Campo<T> fallo(String error) { return new Campo<>(null, error); }
        /** @return true si la conversión fue correcta */
        public boolean esValido() { return error == null; }
    }

    /** Quita espacios extremos y pasa a mayúsculas (tec-001 → TEC-001). @return null si está en blanco */
    public static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    /** Equivale a CHECK (codigo REGEXP '^[A-Z0-9-]{1,12}$'). @param normalizado código ya normalizado */
    public static String errorCodigo(String normalizado) {
        if (normalizado == null) {
            return "el código es obligatorio";
        }
        if (normalizado.length() > CODIGO_MAX) {
            return "el código no puede superar " + CODIGO_MAX + " caracteres";
        }
        if (!PATRON_CODIGO.matcher(normalizado).matches()) {
            return "el código solo admite letras A-Z, dígitos y guiones";
        }
        return null;
    }

    /**
     * Comprueba un texto obligatorio ya recortado con trim().
     * @param campo nombre del campo con artículo, para el mensaje ("el nombre")
     */
    public static String errorTexto(String limpio, String campo, int minimo, int maximo) {
        if (limpio == null || limpio.isEmpty()) {
            return campo + " es obligatorio";
        }
        if (limpio.length() < minimo || limpio.length() > maximo) {
            return campo + " debe tener entre " + minimo + " y " + maximo + " caracteres";
        }
        return null;
    }

    /** Correo opcional: aquí solo llega cuando el usuario ha escrito algo. */
    public static String errorEmail(String limpio) {
        if (limpio.length() > EMAIL_MAX) {
            return "el correo no puede superar " + EMAIL_MAX + " caracteres";
        }
        if (!PATRON_EMAIL.matcher(limpio).matches()) {
            return "el correo no tiene un formato válido (ejemplo: ventas@empresa.es)";
        }
        return null;
    }

    /**
     * Convierte el precio a BigDecimal: mayor que 0, como máximo 2 decimales y
     * dentro de DECIMAL(10,2). Lo devuelve siempre con escala 2 (14.5 → 14.50).
     * <p>Se usa BigDecimal y no double porque 0.1 + 0.2 en double no da 0.3:
     * con dinero no se admiten errores de redondeo.</p>
     */
    public static Campo<BigDecimal> precio(String texto) {
        if (texto == null || texto.isBlank()) {
            return Campo.fallo("el precio es obligatorio");
        }
        try {
            BigDecimal precio = new BigDecimal(texto.trim());
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                return Campo.fallo("el precio debe ser mayor que 0");
            }
            BigDecimal sinCeros = precio.stripTrailingZeros();
            // Se calcula con long: con escalas extremas, precision - scale desborda un int.
            long cifrasEnteras = (long) sinCeros.precision() - sinCeros.scale();
            if (cifrasEnteras > PRECIO_MAX_ENTEROS) {
                return Campo.fallo("el precio no puede superar 99999999.99");
            }
            if (sinCeros.scale() > PRECIO_DECIMALES) {
                return Campo.fallo("el precio admite como máximo " + PRECIO_DECIMALES + " decimales (" + texto.trim() + ")");
            }
            return Campo.ok(precio.setScale(PRECIO_DECIMALES, RoundingMode.UNNECESSARY));
        } catch (NumberFormatException ex) {
            return Campo.fallo("el precio no es un número válido (" + texto.trim() + ")");
        }
    }

    /** Convierte un entero obligatorio. @param campo nombre del campo para el mensaje */
    public static Campo<Integer> entero(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return Campo.fallo(campo + " es obligatorio");
        }
        try {
            return Campo.ok(Integer.parseInt(texto.trim()));
        } catch (NumberFormatException ex) {
            return Campo.fallo(campo + " debe ser un número entero (" + texto.trim() + ")");
        }
    }

    /** Entero mayor que 0 (identificadores de categoría y proveedor). */
    public static Campo<Integer> enteroPositivo(String texto, String campo) {
        Campo<Integer> numero = entero(texto, campo);
        if (numero.esValido() && numero.valor() <= 0) {
            return Campo.fallo(campo + " debe ser un entero positivo");
        }
        return numero;
    }

    /** Entero mayor o igual que 0 (stock y umbral de stock). */
    public static Campo<Integer> enteroNoNegativo(String texto, String campo) {
        Campo<Integer> numero = entero(texto, campo);
        if (numero.esValido() && numero.valor() < 0) {
            return Campo.fallo(campo + " no puede ser negativo");
        }
        return numero;
    }
}
