# DataStock – Fase 3 (UT1 ficheros + UT2 JDBC y MySQL + UT3 componentes)

**Alumno:** José Antonio Pastor Rivera  
**Módulo:** Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Curso 2026–2027  
**Lenguaje:** Java 21 · **Construcción:** Maven · **Interfaz:** consola  
**Dependencias:** Gson 2.11.0 · MySQL Connector/J **8.4.0** · sqlite-jdbc **3.46.1.3** · JUnit **5.10.0** (solo pruebas)

## 1. ¿Qué es DataStock?

Aplicación de consola para que una pequeña empresa gestione su inventario. Es **un único proyecto** que crece por fases:

- **UT1 (opciones 1-6):** importa catálogos JSON y XML a un inventario **en memoria**, los consulta, los exporta y los guarda serializados. Actividades aparte: XPath y RandomAccessFile.
- **UT2 (opción 7):** trabaja con una base de datos **MySQL** real mediante **JDBC**. Mantiene categorías, proveedores y productos, registra entradas y salidas de stock con un procedimiento almacenado, consulta el historial y copia a MySQL los productos de la memoria dentro de una transacción.
- **Actividad comparativa:** la clase independiente `DemoSQLite` usa SQLite, un gestor embebido.
- **UT3 (componentes reutilizables):** no añade menús ni funcionalidades. La lectura del JSON (opción 2) y la búsqueda por código en MySQL (opción 7 > 5 > 2) se han extraído a dos componentes, `LectorProductosJson` y `ConsultaProductoJdbc`, que no dependen de la consola. Ver el [apartado 11](#11-ut3-componentes-reutilizables).

> **Memoria y MySQL son almacenes distintos.** Importar un JSON o XML solo llena la memoria. Los productos pasan a MySQL únicamente con la opción **7.8**, que copia sin borrar ni modificar la memoria. No hay ninguna sincronización automática.

## 2. Requisitos

- JDK **21** o superior. Si en el PATH aparece antes un Java 8, al ejecutar a mano saldrá `UnsupportedClassVersionError`.
- Maven 3.9 o un IDE con integración Maven.
- **MySQL Server 8.0.16 o posterior** (por los `CHECK` del script) y MySQL Workbench o una herramienta equivalente.
- Consola en UTF-8 para ver bien las tildes y el símbolo € (`chcp 65001` en Windows).

## 3. Preparar la base de datos MySQL

1. Con un usuario administrador (por ejemplo `root`), ejecuta el script facilitado **sin modificarlo**: [`material-referencia/datastock-bd-mysql.sql`](material-referencia/datastock-bd-mysql.sql). En Workbench: *File → Open SQL Script* → ⚡. Crea el esquema `datastock`, las tablas `categoria`, `proveedor`, `producto` y `movimiento_stock`, y el procedimiento `registrar_movimiento`. Se puede ejecutar varias veces: no borra datos.
2. Crea un usuario propio para la aplicación, con **mínimo privilegio**. Elige tú la contraseña:

   ```sql
   CREATE USER IF NOT EXISTS 'datastock_app'@'localhost' IDENTIFIED BY 'tu_contraseña';
   GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON datastock.* TO 'datastock_app'@'localhost';
   ```

   Este usuario puede leer, escribir y llamar al procedimiento, pero **no** puede crear ni borrar tablas ni cambiar las restricciones del script, cosa que la práctica prohíbe (§3.1.7).

## 4. Configurar la conexión JDBC (sin publicar contraseñas)

`config/ConexionBD` busca cada dato primero en una **variable de entorno** y, si no existe, en el fichero local **`config/db.properties`**, que está en `.gitignore`. En el repositorio solo está la plantilla [`config/db.properties.example`](config/db.properties.example).

**Opción A: fichero local.** Copia la plantilla como `config/db.properties` y complétala:

```properties
jdbc.url=jdbc:mysql://localhost:3306/datastock?serverTimezone=Europe/Madrid&connectTimeout=5000
jdbc.user=datastock_app
jdbc.password=tu_contraseña
```

**Opción B: variables de entorno.** Tienen prioridad sobre el fichero. En PowerShell, para la sesión actual:

```bash
$env:JDBC_URL="jdbc:mysql://localhost:3306/datastock?serverTimezone=Europe/Madrid&connectTimeout=5000"; $env:JDBC_USER="datastock_app"; $env:JDBC_PASSWORD="tu_contraseña"
```

La opción **7.1** comprueba la conexión y muestra de dónde ha leído la configuración, sin enseñar nunca la contraseña.

## 5. Compilar y ejecutar

Desde la carpeta que contiene `pom.xml`:

```bash
mvn package
```

`mvn package` compila y copia los `.jar` de las dependencias a `target/dependency/`. Después:

```bash
java -cp "target/classes;target/dependency/*" es.iesisidra.datastock.App
```

La clase independiente de SQLite y las actividades de la UT1:

```bash
java -cp "target/classes;target/dependency/*" es.iesisidra.datastock.complementarias.DemoSQLite
```

```bash
java -cp "target/classes;target/dependency/*" es.iesisidra.datastock.complementarias.XPathActividad
```

```bash
java -cp "target/classes;target/dependency/*" es.iesisidra.datastock.complementarias.AccesoAleatorioActividad
```

Pruebas automatizadas (JUnit 5) y documentación de la API (Javadoc), UT3:

```bash
mvn test
```

```bash
mvn javadoc:javadoc
```

`mvn test` ejecuta los 10 tests de `src/test/java`. Los que consultan MySQL se **omiten** (no fallan) si el servidor no está disponible. `mvn javadoc:javadoc` genera la documentación HTML en `target/reports/apidocs/index.html`.

También funciona `mvn exec:java` (y `-Dexec.mainClass=...` para las demás clases). En Linux/macOS el separador del classpath es `:` en lugar de `;`.

**Si Maven falla con `PKIX path building failed`**, es que la red o el antivirus interceptan el HTTPS. En Windows se resuelve indicando a Java que use el almacén de certificados del sistema:

```bash
$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"; mvn package
```

Otras opciones: `-Ddatastock.color=false` (o `NO_COLOR=1`) desactiva los colores ANSI. `-Ddatastock.simularFalloLote=true` es **solo para la prueba CP-JDBC-18**: fuerza un error SQL dentro de la importación 7.8 para comprobar el `rollback()`.

## 6. Menús y requisitos

### Menú principal

| Menú | Operación | Almacén | Requisito |
| --- | --- | --- | --- |
| 1 | Explorar ficheros y directorios | Disco | RF-FIC-01 |
| 2 / 3 | Importar productos desde JSON / XML (**la 2 usa `LectorProductosJson`, UT3**) | Memoria | RF-FIC-02 / 03 |
| 4 | Consultar productos en memoria | Memoria | RF-FIC-04 |
| 5 | Exportar productos | Memoria → disco | RF-FIC-05 |
| 6 | Guardar y recuperar inventario (`.dat`) | Memoria ↔ disco | RF-FIC-06 |
| **7** | **Gestionar base de datos MySQL** | **MySQL** | **UT2** |

### Opción 7 · Gestión MySQL

| Opción | Operación | Requisito | Técnica JDBC |
| --- | --- | --- | --- |
| 7.1 | Comprobar conexión e información de la BD | RF-JDBC-01 | `DatabaseMetaData`, `SELECT COUNT(*)` |
| 7.2 | Categorías: alta, listado, modificar nombre, baja | RF-JDBC-02 | `PreparedStatement`, `executeUpdate()` |
| 7.3 | Proveedores: alta, listado, modificar nombre/email, baja | RF-JDBC-02 | `PreparedStatement`, `setNull` |
| 7.4 | Productos: alta, consulta por código, modificación, baja | RF-JDBC-03 | `PreparedStatement`, `setBigDecimal` |
| 7.5 | Listar (JOIN), buscar por código (**7.5.2 usa `ConsultaProductoJdbc`, UT3**), por nombre (LIKE), stock < umbral | RF-JDBC-04 | `ResultSet` → DTO `ProductoDetalle` |
| 7.6 | Registrar ENTRADA o SALIDA de stock | RF-JDBC-05 | `CallableStatement` → `registrar_movimiento` |
| 7.7 | Historial completo o por producto | RF-JDBC-06 | `ResultSet` → `MovimientoStock` |
| 7.8 | Importar productos de memoria a MySQL | RF-JDBC-07 | Transacción: `setAutoCommit(false)`, `commit`, `rollback` |
| 7.9 | Demostración de commit y rollback | RF-JDBC-08 | Transacción con categorías de prueba |

Convenciones de la consola (apartado 5.1):

- `[OK]` en verde, `[ERROR]` en rojo y `[AVISO]` en amarillo. El prefijo hace que el mensaje se entienda aunque la terminal no muestre colores.
- Los datos se piden uno a uno. Si un valor es incorrecto se explica y se vuelve a pedir; **Intro vacío cancela** la operación sin tocar la base de datos.
- En las modificaciones, Intro conserva el valor actual. En el email del proveedor, `-` lo deja en `NULL`.
- Los borrados y las modificaciones piden confirmación S/N.
- **El stock no se edita en 7.4:** solo cambia con 7.6, para que cada cambio quede en `movimiento_stock`.

### Categorías de demostración (7.9)

La demostración usa IDs de categoría a partir de **9001** y comprueba antes que estén libres. Cada ejecución inserta y confirma dos (por ejemplo 9001 y 9002) y prueba el rollback con una tercera (9003), que no queda guardada. Al final ofrece borrar las dos confirmadas. Para hacerlo a mano, sin afectar a datos reales:

```sql
DELETE FROM categoria WHERE id IN (9001, 9002) AND nombre LIKE 'DEMO-TX %';
```

## 7. Estructura del proyecto

```text
datastock-ut3-joseantonio-pastorrivera-v3-0/
├── pom.xml · README.md · .gitignore
├── config/
│   └── db.properties.example        # Plantilla sin secretos (db.properties local está en .gitignore)
├── datos/
│   ├── ejemplos/ · pruebas/rf01…rf08/   # UT1
│   ├── pruebas/ut2/                  # JSON del recorrido 6.1 y de los lotes; salidas de consola de cada CP
│   ├── pruebas/ut3/                  # Salidas de consola de CP-COMP-01 a 03 y de la regresión
│   ├── sqlite/                       # productos_demo.db (lo crea DemoSQLite; ignorado por Git)
│   └── salidas/
├── docs/registro-de-pruebas-joseantonio-pastorrivera.docx
├── material-referencia/              # Script SQL facilitado (sin modificar) y diagrama ER
└── src/main/java/es/iesisidra/datastock/
    ├── App.java
    ├── config/ConexionBD.java        # UT2 · URL, usuario y contraseña desde entorno o db.properties
    ├── jdbc/ErroresBD.java           # UT2 · traduce SQLException a mensajes comprensibles
    ├── jdbc/ConsultaProductoJdbc.java # UT3 · componente: busca un producto por código (opción 7.5.2)
    ├── dao/                          # UT2 · solo SQL: EsquemaDAO, CategoriaDAO, ProveedorDAO, ProductoDAO, MovimientoDAO
    ├── dto/                          # UT2 · ProductoDetalle (fila con JOIN), InfoBaseDatos
    ├── servicio/
    │   ├── Inventario.java           # UT1 · colección en memoria
    │   ├── ImportadorMySQL.java      # UT2 · RF-JDBC-07, validación + transacción del lote
    │   └── DemoTransaccion.java      # UT2 · RF-JDBC-08
    ├── validacion/
    │   ├── ReglasCampos.java         # Reglas de cada campo (equivalen a las del script SQL), comunes a UT1 y UT2
    │   └── ValidadorProducto.java · DatosProducto.java
    ├── ui/
    │   ├── MenuPrincipal.java        # Opciones 1-7
    │   ├── MenuMySQL.java            # UT2 · opción 7 (7.1, 7.8 y 7.9)
    │   ├── MenuMaestros.java         # UT2 · 7.2 categorías y 7.3 proveedores
    │   ├── MenuProductosBD.java      # UT2 · 7.4 CRUD y 7.5 consultas
    │   ├── MenuStock.java            # UT2 · 7.6 movimientos y 7.7 historial
    │   └── Formulario.java · Mensajes.java   # Pedir datos validados · [OK]/[ERROR]/[AVISO]
    ├── ficheros/LectorProductosJson.java · ResultadoLecturaJson.java   # UT3 · componente: lee y valida un JSON (opción 2)
    ├── ficheros/ · modelo/ · util/   # UT1
    └── complementarias/
        ├── DemoSQLite.java           # UT2 · actividad comparativa (main propio)
        └── XPathActividad · AccesoAleatorioActividad …   # UT1
src/test/java/es/iesisidra/datastock/   # UT3 · JUnit 5: LectorProductosJsonTest, ConsultaProductoJdbcTest
```

## 8. Decisiones de diseño

- **Capas separadas (§8).** Los menús (`ui`) solo piden datos y muestran resultados. Todo el SQL está en `dao`, salvo la consulta por código, que desde la UT3 está en el componente `jdbc/ConsultaProductoJdbc`. Las operaciones con varias sentencias y transacción (`ImportadorMySQL`, `DemoTransaccion`) están en `servicio`. En la UT3 dos operaciones se han convertido en componentes reutilizables (apartado 11).
- **`PreparedStatement` siempre que interviene el usuario.** Nunca se concatena su texto en el SQL: así se evita la inyección SQL. En `LIKE`, los comodines `%` se añaden en Java y se escapan `%` y `_` del texto buscado.
- **Validar en Java y dejar a MySQL como última barrera.** `ReglasCampos` reproduce las restricciones del script (código `^[A-Z0-9-]{1,12}$`, nombre de 3 a 100, `DECIMAL(10,2)` > 0, stock ≥ 0). Antes de insertar se comprueba que existen la categoría y el proveedor. Si aun así MySQL rechaza algo (`PRIMARY KEY`, `UNIQUE`, `CHECK`, `FOREIGN KEY`), la `SQLException` se captura y `ErroresBD` la explica. Nunca se borran registros relacionados para forzar un borrado.
- **Dinero con `BigDecimal`** y `setBigDecimal()`/`getBigDecimal()`: sin los errores de redondeo de `double`.
- **Stock solo por movimientos.** `registrar_movimiento` hace su propia transacción (`START TRANSACTION`, `SELECT … FOR UPDATE`, `UPDATE`, `INSERT`, `COMMIT`/`ROLLBACK`). Java solo lo llama con `CallableStatement` y no abre una transacción exterior que interfiera.
- **Transacción del lote 7.8.** Primero se valida cada producto y se descartan los incorrectos y los duplicados, que no provocan rollback. Después los aceptados se insertan con `setAutoCommit(false)` y `commit()`. Ante un error SQL inesperado, `rollback()` deshace el lote entero. `autoCommit` se restaura en el `finally`.
- **Conexiones cortas y try-with-resources.** Cada operación abre su `Connection` y la cierra con sus `PreparedStatement` y `ResultSet` al terminar, también si hay excepción. `DriverManager.setLoginTimeout(5)` y `connectTimeout=5000` evitan que la aplicación se quede colgada si MySQL no responde.
- **`ResultSet` → objetos.** Cada fila se convierte en `Categoria`, `Proveedor`, `MovimientoStock` o en el DTO `ProductoDetalle`, que añade los nombres de categoría y proveedor obtenidos con JOIN.

## 9. Comparativa MySQL y SQLite

| Pregunta | MySQL (DataStock) | SQLite (`DemoSQLite`) |
| --- | --- | --- |
| ¿Necesita servidor? | Sí: un proceso servidor independiente (servicio `MySQL80`) al que se conectan los clientes por red (TCP, puerto 3306). | No: es un motor **embebido**, una biblioteca dentro de la propia aplicación. |
| ¿Dónde se guardan los datos? | En los ficheros que gestiona el servidor, en su directorio de datos. La aplicación no los toca. | En **un único fichero** local: `datos/sqlite/productos_demo.db`. |
| ¿Cómo se conecta por JDBC? | `jdbc:mysql://localhost:3306/datastock` con usuario y contraseña. | `jdbc:sqlite:datos/sqlite/productos_demo.db`, sin usuario. El fichero se crea al conectar. |
| ¿Qué dependencia usa? | `com.mysql:mysql-connector-j:8.4.0` | `org.xerial:sqlite-jdbc:3.46.1.3` |
| Ventajas | Varios usuarios y aplicaciones a la vez, gestión de usuarios y permisos, procedimientos almacenados, escalabilidad, copias de seguridad y réplica. | Nada que instalar ni administrar, portable (copiar el fichero es copiar la BD), muy ligero y rápido en local. |
| Inconvenientes | Hay que instalarlo, arrancarlo, configurarlo y gestionar credenciales. Más recursos. | Una sola escritura a la vez, sin usuarios ni permisos, sin procedimientos almacenados, tipos de datos flexibles. No sirve para muchos clientes concurrentes por red. |
| ¿Para qué lo elegiría? | Aplicaciones multiusuario o web, como el inventario compartido de una empresa. | Aplicaciones de escritorio o móviles con datos locales, prototipos, pruebas y cachés. |

La API JDBC es la misma: `Connection`, `Statement`, `PreparedStatement` y `ResultSet` funcionan igual. Solo cambian la **dependencia** y la **URL**. Lo que no es estándar sí cambia: por ejemplo, `INSERT OR IGNORE` es de SQLite, y en MySQL sería `INSERT IGNORE`.

**Pool de conexiones.** Un pool (HikariCP, por ejemplo) mantiene abiertas varias conexiones y las presta a quien las pide. Abrir una conexión física cuesta: red, autenticación, sesión. Por eso un pool es útil en aplicaciones con muchas peticiones concurrentes, como un servidor web. DataStock es una aplicación de consola de **un solo usuario**: hace una operación cada vez, a ritmo humano. Por eso le basta con **conexiones cortas y bien cerradas** (try-with-resources): no se acumulan conexiones abiertas, no se agotan las del servidor y el coste de abrir una conexión por operación no se nota.

**Cómo ejecutar `DemoSQLite`:** ver el apartado 5. No está en ningún menú. La primera ejecución crea el fichero e inserta 2 filas. La segunda no inserta nada (`Insertados: 0 | Ya existentes: 2`) y el fichero sigue en disco.

## 9 bis. Diferencias entre memoria (UT1) y MySQL (UT2)

| | Inventario en memoria | MySQL |
| --- | --- | --- |
| Dónde vive | Una `List<Producto>` en `Inventario` | Tabla `producto` del esquema `datastock` |
| Duración | Hasta cerrar la aplicación, salvo que se guarde en `.dat` | Permanente: sigue al cerrar y volver a abrir |
| Quién lo modifica | Opciones 2, 3 y 6 | Opción 7 (submenús 2, 3, 4, 6 y 8) |
| Integridad | `ValidadorProducto` | `ValidadorProducto` + restricciones `PK`, `UNIQUE`, `CHECK` y `FK` |
| Paso de uno a otro | Solo con **7.8** (copia; la memoria no cambia) | No hay paso de MySQL a memoria |

## 10. Pruebas

Las pruebas CP-JDBC-01 a 20 y CP-SQLITE-01 a 03 se ejecutaron contra MySQL 8.0.44. Están en `docs/registro-de-pruebas-joseantonio-pastorrivera.docx`, junto a las 34 de la UT1, con los datos, el resultado esperado, el obtenido, el estado y la evidencia. Las salidas de consola de cada caso están en `datos/pruebas/ut2/` (`LEEME.txt` explica cada fichero). También se reprodujo el recorrido completo del apartado 6.1 (prueba de integración §11.17). Además se comprobó que la UT1 da exactamente la misma salida que en la versión 1.0.

Incidencias encontradas al probar y corregidas:

| ID | Problema | Corrección |
| --- | --- | --- |
| INC-06 | Borrar una categoría o un proveedor con productos mostraba un error genérico (código 1217): solo se reconocía el 1451. | `ErroresBD` reconoce las dos variantes de cada error de clave foránea (1451/1217 y 1452/1216). |
| INC-07 | Un esquema mal escrito en la URL daba un error genérico 1044. | Mensaje propio: con un usuario de mínimo privilegio, MySQL responde «acceso denegado» en vez de «no existe». |
| INC-08 | Maven no podía descargar los drivers (PKIX). | `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (apartado 5). |

### Pruebas de la UT3

| Caso | Datos utilizados | Resultado esperado | Resultado obtenido | Estado |
| --- | --- | --- | --- | --- |
| CP-COMP-01 | Opción 2 con `datos/ejemplos/catalogo_valido.json` (2 válidos) y `catalogo_con_errores.json` (1 válido, 2 inválidos) | Se cargan en memoria los productos válidos y se muestra `[OK]` | `[OK] … Productos añadidos al inventario: 2`, y después `: 1` con un `[AVISO]` y los 2 motivos. La opción 4.1 lista 3 productos | Superada |
| CP-COMP-02 | Opción 2 con `datos/pruebas/ut3/no_existe.json` y `datos/ejemplos/catalogo_malformado.json` | Error controlado en rojo; DataStock sigue funcionando | `[ERROR] … El fichero no existe` y `[ERROR] … no es un JSON válido (línea 2, columna 1)`. El inventario no cambia y la siguiente importación funciona | Superada |
| CP-COMP-03 | Opción 7 > 5 > 2 con `TEC-001` y `zzz-999` | Ficha correcta o aviso; sin excepciones sin controlar | `[OK]` y la ficha de TEC-001 con su categoría y su proveedor; `[AVISO] No existe … ZZZ-999`. Con MySQL inaccesible: `[ERROR]` controlado | Superada |

Las salidas completas están en `datos/pruebas/ut3/` y el registro, en `docs/registro-de-pruebas-joseantonio-pastorrivera.docx`. Además, `mvn test` ejecuta 10 pruebas JUnit 5 de los dos componentes (10 superadas), y una regresión de la UT1 y la UT2 confirmó que su comportamiento no ha cambiado.

## 11. UT3: componentes reutilizables

**Qué se ha hecho.** Solo se han refactorizado las dos operaciones que pide el enunciado:

- `LectorProductosJson` (paquete `ficheros`) recibe un `Path` y devuelve un `ResultadoLecturaJson` con los `Producto` válidos y el motivo de cada rechazado. Si el fichero no existe o el JSON es incorrecto, lanza `ImportacionException`.
- `ConsultaProductoJdbc` (paquete `jdbc`) recibe un código y devuelve un `Optional<ProductoDetalle>`, vacío si el producto no existe. Usa `PreparedStatement`, `ResultSet` y la conexión de `ConexionBD`, y cierra todo con try-with-resources.

Ninguno de los dos escribe en la consola ni lee del teclado.

**¿Qué ventaja tiene separar la lectura JSON del menú? ¿Qué coste tiene?** El componente se puede reutilizar sin el menú: desde otra aplicación, desde una actividad (`AccesoAleatorioActividad` lo usa a través de `ImportadorJson`) o desde una prueba JUnit, que comprueba el resultado sin simular el teclado. Cada clase tiene una sola responsabilidad: el lector lee y valida, y el menú pregunta, añade al inventario y muestra el resultado. Un cambio en el formato del fichero no afecta a la interfaz, y un cambio en la interfaz no afecta a la lectura. El inconveniente es que hay más clases (`ResultadoLecturaJson`, el adaptador `ImportadorJson`) y el flujo cuesta más de seguir. También hay que repartir bien el trabajo. Por ejemplo, el lector no conoce el inventario, así que los productos que ya están en memoria los detecta el menú, y por eso esa incidencia ya no indica la posición en el fichero. En un programa pequeño ese coste se nota, y por eso solo se han separado las dos operaciones pedidas.

**¿Por qué separar una consulta JDBC de la presentación por consola?** La consulta solo debe ocuparse de SQL y de JDBC: abrir la conexión, preparar la sentencia, convertir la fila en un objeto y cerrar los recursos. Si además imprimiera, no se podría usar desde una interfaz gráfica, una web o un test, y para cambiar un mensaje habría que tocar el acceso a datos. Como el componente devuelve un `Optional` y propaga la `SQLException`, quien lo llama decide cómo presentar el resultado. En DataStock, el menú muestra `[OK]` y la ficha, un `[AVISO]` si el producto no existe o un `[ERROR]` traducido por `ErroresBD`. Además, `ProductoDAO.buscarPorCodigo` delega en el componente, así que la sentencia SQL está en un solo sitio para 7.4, 7.5.2 y 7.6.

**Herramientas utilizadas.**

- **IDE** (IntelliJ IDEA o VS Code con Java): editar, compilar sobre la marcha, depurar y navegar entre clases.
- **Maven**: gestiona las dependencias (Gson, Connector/J, JUnit), compila con Java 21 y ejecuta las pruebas en `mvn test` y `mvn package`. Con `maven-javadoc-plugin` genera también la documentación.
- **Javadoc**: los comentarios `/** … */` de las dos clases y de sus métodos públicos (finalidad, `@param`, `@return`, `@throws`) se convierten en HTML con `mvn javadoc:javadoc`. Es la documentación de la API que necesita quien quiera reutilizar el componente sin leer su código.
- **JUnit 5**: 10 pruebas automatizadas (`LectorProductosJsonTest` y `ConsultaProductoJdbcTest`). Entre otras cosas, comprueban que el lector no escribe nada en la consola. Las de MySQL usan `Assumptions` para omitirse si no hay servidor.

**Opciones de DataStock modificadas.** Solo dos, sin añadir menús:

- La **opción 2** del menú principal (Importar productos desde JSON) usa `LectorProductosJson`.
- La **opción 7 > 5 > 2** (Buscar producto por código en MySQL) usa `ConsultaProductoJdbc`.

El resto de las opciones de la UT1 y la UT2 no cambia.

| CE de RA6 | Evidencia |
| --- | --- |
| a) Ventajas e inconvenientes | Este apartado |
| b) Herramientas | IDE, Maven, Javadoc y JUnit (arriba) y `pom.xml` |
| c) Componente de ficheros | `ficheros/LectorProductosJson.java` |
| d) Componente de BD con conector | `jdbc/ConsultaProductoJdbc.java` |
| h) Probados y documentados | CP-COMP-01 a 03 (apartado 10), JUnit en `src/test/java`, Javadoc |
| i) Integración | Opciones 2 y 7.5.2 (`ui/MenuPrincipal.java` y `ui/MenuProductosBD.java`) |

## 12. Uso de herramientas de IA

Para las fases 2 y 3 se ha utilizado un asistente de IA (Claude) como apoyo para el diseño, la generación del código JDBC y de los componentes de la UT3, la ejecución de las pruebas y la redacción de la documentación. Todo el código se ha revisado, ejecutado y probado contra MySQL. Las decisiones de diseño y su justificación están documentadas en este README para poder explicarlas.
