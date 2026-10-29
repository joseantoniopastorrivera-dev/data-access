package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.validacion.DatosProducto;
import es.iesisidra.datastock.validacion.ValidadorProducto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Una importación en curso, independiente del formato del fichero.
 * <p>JSON y XML solo se encargan de leer su documento y traducir cada
 * registro a {@link DatosProducto}; esta clase valida, detecta duplicados,
 * añade al inventario y lleva el resumen. Se crea una por cada importación,
 * porque guarda los códigos aceptados de ese fichero concreto.</p>
 */
public class ProcesoImportacion {
    private final Inventario inventario;
    private final ValidadorProducto validador = new ValidadorProducto();
    private final Set<String> codigosDelFichero = new HashSet<>();
    private final ResultadoImportacion resultado = new ResultadoImportacion();

    public ProcesoImportacion(Inventario inventario) {
        this.inventario = inventario;
    }

    /** Valida un producto, comprueba duplicados y lo añade o registra la incidencia. */
    public void procesar(DatosProducto datos) {
        resultado.sumarEncontrado();
        List<String> errores = new ArrayList<>();
        Producto producto = validador.validar(datos, errores);

        if (producto != null && codigosDelFichero.contains(producto.getCodigo())) {
            errores.add("código duplicado dentro del mismo fichero");
        } else if (producto != null && !inventario.agregar(producto)) {
            errores.add("código duplicado: ya existe en el inventario");
        }

        if (errores.isEmpty()) {
            codigosDelFichero.add(producto.getCodigo());
            resultado.sumarImportado();
        } else {
            resultado.anadirIncidencia(identificar(datos) + ": " + String.join("; ", errores));
        }
    }

    /** Cuenta un registro que ni siquiera tiene forma de producto y anota el motivo. */
    public void rechazar(int posicion, String motivo) {
        resultado.sumarEncontrado();
        resultado.anadirIncidencia("Producto " + posicion + ": " + motivo);
    }

    public ResultadoImportacion getResultado() {
        return resultado;
    }

    /** "Producto 3 (CAB-005)", o solo "Producto 3" si no tiene código. */
    private String identificar(DatosProducto datos) {
        String codigo = ValidadorProducto.normalizarCodigo(datos.codigo());
        return "Producto " + datos.posicion() + (codigo == null ? "" : " (" + codigo + ")");
    }
}
