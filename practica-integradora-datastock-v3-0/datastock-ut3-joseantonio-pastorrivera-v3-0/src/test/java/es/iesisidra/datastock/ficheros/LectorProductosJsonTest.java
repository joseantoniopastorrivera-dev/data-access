package es.iesisidra.datastock.ficheros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.iesisidra.datastock.modelo.Producto;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UT3 · Pruebas automatizadas del componente {@link LectorProductosJson}.
 * <p>No necesitan MySQL: usan los JSON de ejemplo de {@code datos/}. Maven ejecuta
 * los tests desde la carpeta del proyecto, así que las rutas relativas funcionan.</p>
 */
class LectorProductosJsonTest {
    private final LectorProductosJson lector = new LectorProductosJson();

    @Test
    @DisplayName("CP-COMP-01 · Un JSON válido devuelve todos sus productos como objetos Producto")
    void leeJsonValido() throws ImportacionException {
        ResultadoLecturaJson resultado = lector.leer(Path.of("datos/ejemplos/catalogo_valido.json"));

        assertEquals(2, resultado.encontrados());
        assertEquals(2, resultado.productos().size());
        assertEquals(0, resultado.rechazados());
        assertTrue(resultado.incidencias().isEmpty());
        Producto primero = resultado.productos().get(0);
        assertEquals("TEC-001", primero.getCodigo());
        assertEquals(new BigDecimal("19.95"), primero.getPrecio());
    }

    @Test
    @DisplayName("Datos inválidos: se descartan con su motivo y se devuelven los válidos")
    void descartaProductosInvalidos() throws ImportacionException {
        ResultadoLecturaJson resultado = lector.leer(Path.of("datos/ejemplos/catalogo_con_errores.json"));

        assertEquals(3, resultado.encontrados());
        assertEquals(1, resultado.productos().size());
        assertEquals("CAB-005", resultado.productos().get(0).getCodigo());
        assertEquals(2, resultado.rechazados());
        assertTrue(resultado.incidencias().get(0).contains("precio"));
        assertTrue(resultado.incidencias().get(1).contains("duplicado dentro del mismo fichero"));
    }

    @Test
    @DisplayName("CP-COMP-02 · Un fichero inexistente lanza ImportacionException")
    void ficheroInexistente() {
        ImportacionException ex = assertThrows(ImportacionException.class,
                () -> lector.leer(Path.of("datos/no_existe.json")));
        assertTrue(ex.getMessage().contains("no existe"));
    }

    @Test
    @DisplayName("CP-COMP-02 · Un JSON mal formado lanza ImportacionException con la posición")
    void jsonMalFormado() {
        ImportacionException ex = assertThrows(ImportacionException.class,
                () -> lector.leer(Path.of("datos/ejemplos/catalogo_malformado.json")));
        assertTrue(ex.getMessage().contains("no es un JSON válido"));
        assertTrue(ex.getMessage().contains("línea"));
    }

    @Test
    @DisplayName("Una ruta nula lanza ImportacionException, no NullPointerException")
    void rutaNula() {
        assertThrows(ImportacionException.class, () -> lector.leer(null));
    }

    @Test
    @DisplayName("El componente no escribe nada en la consola")
    void noDependeDeLaConsola() throws ImportacionException {
        PrintStream original = System.out;
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturado));
        try {
            lector.leer(Path.of("datos/ejemplos/catalogo_con_errores.json"));
        } finally {
            System.setOut(original);
        }
        assertEquals(0, capturado.size());
    }
}
