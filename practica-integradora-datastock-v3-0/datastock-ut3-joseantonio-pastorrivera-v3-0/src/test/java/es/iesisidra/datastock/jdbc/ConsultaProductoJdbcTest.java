package es.iesisidra.datastock.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.dto.ProductoDetalle;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UT3 · Pruebas automatizadas del componente {@link ConsultaProductoJdbc}.
 * <p>Las que consultan MySQL necesitan el servidor arrancado y {@code config/db.properties}.
 * Si no hay conexión, JUnit las marca como <i>omitidas</i> (Assumptions) en vez de
 * fallar: así {@code mvn package} funciona también en un equipo sin MySQL.</p>
 * <p>Usan TEC-001, que el registro de pruebas de la UT2 dejó dado de alta en MySQL.</p>
 */
class ConsultaProductoJdbcTest {
    private final ConsultaProductoJdbc consulta = new ConsultaProductoJdbc();

    @Test
    @DisplayName("CP-COMP-03 · Un código existente devuelve la ficha con categoría y proveedor")
    void codigoExistente() throws SQLException {
        assumeTrue(hayMySQL(), "MySQL no disponible: prueba omitida");

        Optional<ProductoDetalle> producto = consulta.buscarPorCodigo("TEC-001");

        assertTrue(producto.isPresent());
        assertEquals("TEC-001", producto.get().codigo());
        assertTrue(producto.get().categoriaNombre() != null && producto.get().proveedorNombre() != null);
    }

    @Test
    @DisplayName("El código se normaliza: ' tec-001 ' encuentra TEC-001")
    void codigoNormalizado() throws SQLException {
        assumeTrue(hayMySQL(), "MySQL no disponible: prueba omitida");
        assertTrue(consulta.buscarPorCodigo(" tec-001 ").isPresent());
    }

    @Test
    @DisplayName("CP-COMP-03 · Un código inexistente devuelve Optional vacío, sin excepción")
    void codigoInexistente() throws SQLException {
        assumeTrue(hayMySQL(), "MySQL no disponible: prueba omitida");
        assertTrue(consulta.buscarPorCodigo("ZZZ-999").isEmpty());
    }

    @Test
    @DisplayName("Un código vacío o nulo se rechaza antes de conectar")
    void codigoVacio() {
        assertThrows(IllegalArgumentException.class, () -> consulta.buscarPorCodigo(null));
        assertThrows(IllegalArgumentException.class, () -> consulta.buscarPorCodigo("   "));
    }

    /** @return true si se puede abrir una conexión con la configuración de DataStock */
    private static boolean hayMySQL() {
        try (Connection con = ConexionBD.obtenerConexion()) {
            return con.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }
}
