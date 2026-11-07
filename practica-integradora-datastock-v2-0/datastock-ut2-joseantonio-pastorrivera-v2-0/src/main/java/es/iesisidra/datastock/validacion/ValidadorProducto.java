package es.iesisidra.datastock.validacion;

import es.iesisidra.datastock.modelo.Producto;
import java.math.BigDecimal;
import java.util.List;

/**
 * Valida un producto completo (enunciado UT1, apartado 6, y UT2, apartado 3.1).
 * <p>Convierte los textos de {@link DatosProducto} en tipos Java y comprueba
 * cada regla con {@link ReglasCampos}. Si algo falla, añade un mensaje a la lista
 * de errores. Lo usan la importación JSON y XML (UT1) y la importación de
 * memoria a MySQL (UT2), así que las tres aplican exactamente las mismas reglas.</p>
 */
public class ValidadorProducto {

    /**
     * Valida los datos y, si todo es correcto, construye el Producto.
     * @param datos   valores leídos del fichero o de la memoria
     * @param errores lista donde se añaden los motivos de rechazo (uno por regla incumplida)
     * @return el producto si es válido; {@code null} si se ha añadido algún error
     */
    public Producto validar(DatosProducto datos, List<String> errores) {
        int erroresIniciales = errores.size();

        String codigo = ReglasCampos.normalizarCodigo(datos.codigo());
        anadir(errores, ReglasCampos.errorCodigo(codigo));

        String nombre = datos.nombre() == null ? null : datos.nombre().trim();
        anadir(errores, ReglasCampos.errorTexto(nombre, "el nombre",
                ReglasCampos.NOMBRE_PRODUCTO_MIN, ReglasCampos.NOMBRE_PRODUCTO_MAX));

        ReglasCampos.Campo<BigDecimal> precio = ReglasCampos.precio(datos.precio());
        anadir(errores, precio.error());

        ReglasCampos.Campo<Integer> stock = ReglasCampos.entero(datos.stock(), "stock");
        anadir(errores, stock.error());
        if (stock.esValido() && stock.valor() < 0) {
            errores.add("el stock no puede ser negativo");
        }
        ReglasCampos.Campo<Integer> categoriaId = ReglasCampos.enteroPositivo(datos.categoriaId(), "categoriaId");
        anadir(errores, categoriaId.error());
        ReglasCampos.Campo<Integer> proveedorId = ReglasCampos.enteroPositivo(datos.proveedorId(), "proveedorId");
        anadir(errores, proveedorId.error());

        if (errores.size() > erroresIniciales) {
            return null;
        }
        return new Producto(codigo, nombre, precio.valor(), stock.valor(), categoriaId.valor(), proveedorId.valor());
    }

    /**
     * UT2 · Vuelve a validar un producto que ya está en memoria antes de copiarlo a MySQL.
     * <p>Se pasa a texto para reutilizar exactamente las mismas reglas que al importar
     * un fichero; así no hay dos versiones de la validación que puedan divergir.</p>
     */
    public Producto validar(Producto producto, List<String> errores) {
        DatosProducto datos = new DatosProducto(0, producto.getCodigo(), producto.getNombre(),
                producto.getPrecio() == null ? null : producto.getPrecio().toPlainString(),
                String.valueOf(producto.getStock()), String.valueOf(producto.getCategoriaId()),
                String.valueOf(producto.getProveedorId()));
        return validar(datos, errores);
    }

    /** Se mantiene por compatibilidad con el código de la UT1. @see ReglasCampos#normalizarCodigo */
    public static String normalizarCodigo(String codigo) {
        return ReglasCampos.normalizarCodigo(codigo);
    }

    private static void anadir(List<String> errores, String error) {
        if (error != null) {
            errores.add(error);
        }
    }
}
