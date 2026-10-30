package es.iesisidra.datastock.ficheros;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de una importación cuyo documento se pudo leer:
 * productos encontrados, importados, rechazados y motivos de rechazo.
 * <p>Lo usan tanto la importación JSON como la XML. El menú lo muestra.</p>
 */
public class ResultadoImportacion {
    private int encontrados;
    private int importados;
    private final List<String> incidencias = new ArrayList<>();

    public void sumarEncontrado() { encontrados++; }
    public void sumarImportado() { importados++; }
    public void anadirIncidencia(String incidencia) { incidencias.add(incidencia); }

    public int getEncontrados() { return encontrados; }
    public int getImportados() { return importados; }
    /** Los rechazados se calculan para que nunca puedan quedar descuadrados. */
    public int getRechazados() { return encontrados - importados; }
    /** @return copia de solo lectura, para que nadie modifique el resumen desde fuera */
    public List<String> getIncidencias() { return List.copyOf(incidencias); }
}
