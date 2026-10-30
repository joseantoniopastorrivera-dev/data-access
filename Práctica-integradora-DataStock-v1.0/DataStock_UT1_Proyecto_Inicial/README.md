# DataStock – Fase 1 (UT1 · Ficheros y formatos de intercambio)

**Alumno:** José Antonio Pastor Rivera  
**Módulo:** Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Curso 2026–2027  
**Lenguaje:** Java 21 · **Construcción:** Maven · **Interfaz:** consola · **Dependencia:** Gson 2.11.0

## 1. ¿Qué es DataStock?

Aplicación de consola para que una pequeña empresa gestione su inventario. En esta primera fase:

- importa catálogos de productos desde **JSON** (Gson) y **XML** (DOM), valida cada registro y explica los rechazos;
- consulta y busca los productos en memoria;
- exporta el inventario a JSON y XML;
- guarda y recupera el inventario mediante **serialización** Java (`.dat`);
- incluye dos actividades independientes: consultas **XPath** y acceso aleatorio con **RandomAccessFile**.

En la UT1 no hay base de datos: los productos viven en una colección Java compartida por todas las opciones.

## 2. Requisitos

- JDK **21** o superior (`java -version`). Si en el PATH aparece antes un Java 8, al ejecutar a mano saldrá `UnsupportedClassVersionError`.
- Maven, o un IDE con integración Maven (la primera vez descarga Gson).
- Consola en UTF-8 para ver bien tildes y el símbolo €.

## 3. Cómo compilar y ejecutar

Desde la carpeta que contiene `pom.xml`:

```bash
mvn clean compile
mvn exec:java
```

Actividades complementarias (no están en el menú principal):

```bash
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.XPathActividad
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.AccesoAleatorioActividad
```

Sin el plugin `exec` (por ejemplo, si Maven no puede descargarlo), tras `mvn compile`, desde la consola de Windows (cmd):

```bash
java -cp "target/classes;%USERPROFILE%/.m2/repository/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar" es.iesisidra.datastock.App
```

(En Linux/macOS el separador del classpath es `:` en lugar de `;`.) Si `mvn clean` tampoco puede descargar su plugin, basta con borrar la carpeta `target/` a mano y ejecutar `mvn compile`.

Para desactivar los colores ANSI: `-Ddatastock.color=false` o la variable de entorno `NO_COLOR=1`. Las rutas relativas se resuelven desde la carpeta del `pom.xml`.

## 4. Menús y requisitos

| Menú | Operación | Requisito |
|---|---|---|
| 1 | Explorar ficheros y directorios | RF-FIC-01 |
| 2 | Importar productos desde JSON | RF-FIC-02 |
| 3 | Importar productos desde XML | RF-FIC-03 |
| 4 → 1 / 2 / 3 | Listar todos · buscar por código · buscar por nombre | RF-FIC-04 |
| 5 → 1 / 2 | Exportar a JSON · exportar a XML | RF-FIC-05 |
| 6 → 1 / 2 | Guardar inventario `.dat` · recuperar inventario `.dat` | RF-FIC-06 |
| `XPathActividad` | Consultas por código, categoría, precio superior y contador | RF-FIC-07 |
| `AccesoAleatorioActividad` | Generar `stock.bin`, consultar y modificar registros de 36 bytes | RF-FIC-08 |

Importar JSON o XML **añade** los productos válidos y no duplicados; recuperar un `.dat` **sustituye** el inventario, previa confirmación.

## 5. Estructura del proyecto

```text
DataStock_UT1_Proyecto_Inicial/
├── pom.xml · README.md · .gitignore
├── datos/
│   ├── ejemplos/            # Catálogos de ejemplo facilitados (no se modifican)
│   ├── pruebas/rf01 … rf08/ # Ficheros de prueba válidos e inválidos y evidencias, por requisito
│   └── salidas/             # Exportaciones de trabajo (ignoradas por Git)
├── docs/
│   └── Registro_de_Pruebas_JoseAntonio_PastorRivera.docx
├── material_referencia/     # Script SQL y diagrama ER para la UT2
└── src/main/java/es/iesisidra/datastock/
    ├── App.java                     # Punto de entrada: crea el inventario y el menú
    ├── modelo/                      # Producto, Categoria, Proveedor, MovimientoStock (facilitados)
    ├── servicio/Inventario.java     # Colección común: agregar, buscar, listar, sustituir
    ├── validacion/
    │   ├── DatosProducto.java       # Registro leído del fichero, aún como texto
    │   └── ValidadorProducto.java   # Reglas del apartado 6, comunes a JSON, XML y .dat
    ├── ficheros/
    │   ├── ExploradorRutas.java              # RF-FIC-01
    │   ├── ImportadorCatalogo.java (interfaz)  ImportadorJson.java · ImportadorXml.java
    │   ├── ProcesoImportacion.java           # Validar, detectar duplicados, añadir, resumir
    │   ├── ResultadoImportacion.java · ImportacionException.java
    │   ├── ExportadorCatalogo.java (interfaz)  ExportadorJson.java · ExportadorXml.java
    │   ├── SerializadorInventario.java       # RF-FIC-06, con filtro de deserialización
    │   ├── ComprobadorRutas.java · EscrituraAtomica.java · XmlSeguro.java
    │   └── ExportacionException.java
    ├── ui/MenuPrincipal.java        # Solo interacción: pide datos, delega y muestra resultados
    ├── util/                        # LectorConsola, ColoresConsola (facilitados)
    └── complementarias/
        ├── XPathActividad.java · ConsultasXPath.java              # RF-FIC-07
        └── AccesoAleatorioActividad.java · FicheroStock.java      # RF-FIC-08
```

## 6. Decisiones de diseño principales

- **Separación de responsabilidades.** El `switch` de los menús solo delega. Las clases de `ficheros` no hablan con el usuario: devuelven resultados o lanzan excepciones, y el menú los presenta.
- **Un único validador para todos los formatos.** JSON, XML y `.dat` se traducen a `DatosProducto` (campos como texto) y pasan por `ValidadorProducto`. Así los tres formatos producen productos equivalentes.
- **Dos tipos de error en la importación.** Un error de documento (fichero inaccesible, mal formado, raíz incorrecta) lanza `ImportacionException` antes de tocar el inventario. Un error de producto se anota en `ResultadoImportacion` y la importación continúa.
- **El código se normaliza** (espacios extremos y mayúsculas) antes de validar el patrón `[A-Z0-9-]+`, porque la regla dice "único sin distinguir mayúsculas".
- **Seguridad.**
  - JSON en modo estricto: Gson es permisivo por defecto.
  - XML con el DOCTYPE prohibido (protección XXE).
  - `.dat` con un `ObjectInputFilter` que solo admite `ArrayList`, `Producto` y sus números.
  - XPath sin concatenar texto del usuario: se usan variables `$categoria` y `$precio`, y el código se compara en Java.
- **Escritura atómica.** Las exportaciones y el guardado escriben primero en un temporal y después lo mueven sobre el destino, de modo que un fallo nunca deja a medias el fichero anterior.
- **RF-FIC-08.** La actividad tiene su propio `main` y, por tanto, su propia memoria. Primero carga un catálogo con los importadores de DataStock y genera `stock.bin` como copia independiente. Modificar el fichero no cambia los productos en memoria.

## 7. Pruebas

Las 34 pruebas de aceptación del enunciado (CP-FIC, CP-JSON, CP-XML, CP-CON, CP-EXP, CP-SER, CP-XPATH, CP-RAF) se ejecutaron y están documentadas en `docs/Registro_de_Pruebas_JoseAntonio_PastorRivera.docx`, con los datos usados, el resultado esperado, el obtenido y el estado. Todas están **SUPERADAS**. El registro incluye también las incidencias detectadas y cómo se corrigieron.

Los ficheros de cada prueba están en `datos/pruebas/rfXX/`. En `rf05`, `rf06` y `rf08` hay un `LEEME.txt` que explica cada fichero.

### Incidencias corregidas tras la revisión final

Una revisión completa del proyecto encontró cinco incidencias. Están corregidas, tienen sus ficheros de prueba en `datos/pruebas/incidencias/` y figuran en el apartado 3 del registro de pruebas. Después de corregirlas se repitieron las 34 pruebas de aceptación.

| ID | Problema | Corrección |
|---|---|---|
| INC-01 | Un precio enorme (`1E999999999`) se importaba y la aplicación se cerraba por falta de memoria al listarlo o exportarlo. También se aceptaban precios con más de 2 decimales. | El precio se limita a `DECIMAL(10,2)`, como en la base de datos de la UT2, y se normaliza a 2 decimales. |
| INC-02 | `1E+3` se exportaba como `1E+3` en JSON y como `1000` en XML. | Con la normalización de INC-01, en ambos formatos sale `1000.00`. |
| INC-03 | Un JSON que no estaba en UTF-8 daba un falso error de sintaxis. | Se detecta la codificación incorrecta y se explica. |
| INC-04 | Un `.dat` con datos primitivos mostraba «No se pudo leer el fichero: null». | Mensaje propio para `OptionalDataException`. |
| INC-05 | El análisis seguro de XML estaba duplicado en la importación y en XPath. | Método común `XmlSeguro.parsear`. |

## 8. Uso de herramientas de IA

Durante el desarrollo he utilizado un asistente de IA (Claude, de Anthropic) como apoyo de aprendizaje:

- **Tutoría:** explicación de los requisitos, de las API (NIO, Gson, DOM, XPath, serialización, RandomAccessFile) y de las alternativas de diseño, con pistas y esqueletos para orientar la implementación.
- **Soluciones de referencia explicadas:** el asistente propuso la implementación de los requisitos, explicada paso a paso. La he estudiado, he ejecutado las pruebas de aceptación y puedo justificar cada decisión (por ejemplo, el modo estricto de Gson, la protección XXE, el filtro de deserialización o el cálculo de desplazamientos de RF-FIC-08).
- **Revisión:** detección de errores de concepto y de casos límite no cubiertos.

Las decisiones finales, las pruebas registradas y la comprensión del código son responsabilidad mía.
