package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;

/**
 * RF-FIC-02 · Importar un catálogo JSON al inventario.
 * <p>UT3: la lectura y la validación del JSON se han trasladado al componente
 * {@link LectorProductosJson}. Esta clase solo adapta ese componente a la interfaz
 * {@link ImportadorCatalogo} para que la sigan usando las actividades de la UT1
 * ({@code AccesoAleatorioActividad}), que tratan igual JSON y XML. No repite código
 * de lectura: lo pide al componente y añade al inventario lo que recibe.</p>
 * <p>La opción 2 del menú principal ya no pasa por aquí: llama directamente al componente.</p>
 */
public class ImportadorJson implements ImportadorCatalogo {
    private final LectorProductosJson lector = new LectorProductosJson();

    /**
     * @param textoRuta  ruta del fichero escrita por el usuario
     * @param inventario colección común donde se añaden los productos válidos
     * @return resumen de la importación (el documento se pudo leer)
     * @throws ImportacionException si el documento entero no puede procesarse; el inventario no cambia
     */
    @Override
    public ResultadoImportacion importar(String textoRuta, Inventario inventario) throws ImportacionException {
        ResultadoLecturaJson lectura = lector.leer(ComprobadorRutas.ficheroLegible(textoRuta));

        ResultadoImportacion resultado = new ResultadoImportacion();
        for (int i = 0; i < lectura.encontrados(); i++) {
            resultado.sumarEncontrado();
        }
        lectura.incidencias().forEach(resultado::anadirIncidencia);
        for (Producto producto : lectura.productos()) {
            if (inventario.agregar(producto)) {
                resultado.sumarImportado();
            } else {
                resultado.anadirIncidencia("Producto " + producto.getCodigo() + ": código duplicado: ya existe en el inventario");
            }
        }
        return resultado;
    }
}
