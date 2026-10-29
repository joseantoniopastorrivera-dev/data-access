package es.iesisidra.datastock.validacion;

import es.iesisidra.datastock.modelo.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas comunes de los campos de un producto (enunciado, apartado 6).
 * <p>Convierte los textos de {@link DatosProducto} en tipos Java y comprueba
 * cada regla. Si algo falla, añade un mensaje a la lista de errores.
 * La usan la importación JSON y la XML, así que ambas aplican las mismas reglas.</p>
 */
public class ValidadorProducto {
    private static final int CODIGO_MAX = 12;
    private static final int NOMBRE_MIN = 3;
    private static final int NOMBRE_MAX = 100;
    private static final Pattern PATRON_CODIGO = Pattern.compile("[A-Z0-9-]+");

    /**
     * Valida los datos y, si todo es correcto, construye el Producto.
     * @param datos   valores leídos del fichero
     * @param errores lista donde se añaden los motivos de rechazo (uno por regla incumplida)
     * @return el producto si es válido; {@code null} si se ha añadido algún error
     */
    public Producto validar(DatosProducto datos, List<String> errores) {
        int erroresIniciales = errores.size();

        String codigo = normalizarCodigo(datos.codigo());
        if (codigo == null) {
            errores.add("el código es obligatorio");
        } else if (codigo.length() > CODIGO_MAX) {
            errores.add("el código no puede superar " + CODIGO_MAX + " caracteres");
        } else if (!PATRON_CODIGO.matcher(codigo).matches()) {
            errores.add("el código solo admite letras A-Z, dígitos y guiones");
        }

        String nombre = datos.nombre() == null ? null : datos.nombre().trim();
        if (nombre == null || nombre.isEmpty()) {
            errores.add("el nombre es obligatorio");
        } else if (nombre.length() < NOMBRE_MIN || nombre.length() > NOMBRE_MAX) {
            errores.add("el nombre debe tener entre " + NOMBRE_MIN + " y " + NOMBRE_MAX + " caracteres");
        }

        BigDecimal precio = leerPrecio(datos.precio(), errores);

        Integer stock = leerEntero(datos.stock(), "stock", errores);
        if (stock != null && stock < 0) {
            errores.add("el stock no puede ser negativo");
        }
        Integer categoriaId = leerEntero(datos.categoriaId(), "categoriaId", errores);
        if (categoriaId != null && categoriaId <= 0) {
            errores.add("categoriaId debe ser un entero positivo");
        }
        Integer proveedorId = leerEntero(datos.proveedorId(), "proveedorId", errores);
        if (proveedorId != null && proveedorId <= 0) {
            errores.add("proveedorId debe ser un entero positivo");
        }

        if (errores.size() > erroresIniciales) {
            return null;
        }
        return new Producto(codigo, nombre, precio, stock, categoriaId, proveedorId);
    }

    /**
     * Quita los espacios extremos y pasa el código a mayúsculas.
     * @return el código normalizado, o {@code null} si no hay código
     */
    public static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    /** Convierte el precio a BigDecimal y comprueba que es mayor que 0. */
    private BigDecimal leerPrecio(String texto, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add("el precio es obligatorio");
            return null;
        }
        try {
            BigDecimal precio = new BigDecimal(texto.trim());
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                errores.add("el precio debe ser mayor que 0");
                return null;
            }
            return precio;
        } catch (NumberFormatException ex) {
            errores.add("el precio no es un número válido (" + texto + ")");
            return null;
        }
    }

    /** Convierte un campo entero; si falta o no es entero, añade el error y devuelve null. */
    private Integer leerEntero(String texto, String campo, List<String> errores) {
        if (texto == null || texto.isBlank()) {
            errores.add(campo + " es obligatorio");
            return null;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            errores.add(campo + " debe ser un número entero (" + texto + ")");
            return null;
        }
    }
}
