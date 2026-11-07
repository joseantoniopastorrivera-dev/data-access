package es.iesisidra.datastock.dto;

import java.util.List;

/**
 * Resultado de la comprobación de la opción 7.1 (RF-JDBC-01).
 * @param baseDatos        esquema al que se ha conectado (catálogo actual)
 * @param gestor           nombre del SGBD, según DatabaseMetaData
 * @param version          versión del servidor
 * @param driver           nombre y versión del driver JDBC
 * @param tablasQueFaltan  tablas obligatorias que no existen (vacía si están las cuatro)
 * @param procedimiento    true si existe registrar_movimiento
 * @param productos        número de filas de la tabla producto, o -1 si la tabla no existe
 */
public record InfoBaseDatos(String baseDatos, String gestor, String version, String driver,
                            List<String> tablasQueFaltan, boolean procedimiento, long productos) { }
