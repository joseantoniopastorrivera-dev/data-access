# DataStock – Proyecto inicial (UT1)

**Módulo:** Acceso a Datos · 2.º DAM · IES Isidra de Guzmán · Curso 2026–2027  
**Lenguaje:** Java 21 · **Construcción:** Maven · **Interfaz:** consola

## 1. ¿Qué es este proyecto?

DataStock es una práctica **individual e integradora** que evolucionará durante tres unidades:

- **UT1 – Ficheros:** importar JSON/XML, consultar en memoria, exportar, serializar y desarrollar dos actividades independientes (XPath y `RandomAccessFile`).
- **UT2 – JDBC:** incorporar MySQL, operaciones SQL y transacciones.
- **UT3 – Componentes:** reorganizar el acceso a datos en componentes reutilizables.

**Esta entrega inicial solo prepara la UT1.** El código entregado **no resuelve** los requisitos funcionales: los menús muestran mensajes `[PENDIENTE]` hasta que se implementen.

## 2. Requisitos previos

- JDK **21** instalado y seleccionado en el IDE (`java -version`).
- Maven instalado, o un IDE con integración Maven.
- Consola con UTF-8 para ver correctamente tildes y símbolos.

## 3. Cómo abrir y ejecutar

1. Descomprime el ZIP y abre **la carpeta que contiene `pom.xml`** como proyecto Maven en IntelliJ IDEA, Eclipse o VS Code.
2. Espera a que Maven descargue la dependencia Gson (requiere conexión a Internet la primera vez).
3. Ejecuta `es.iesisidra.datastock.App` desde el IDE, o en una terminal situada en la raíz:

```bash
mvn clean compile
mvn exec:java
```

Para ejecutar las actividades complementarias (todavía pendientes de desarrollar):

```bash
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.XPathActividad
mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.AccesoAleatorioActividad
```

> Si el IDE muestra caracteres de control ANSI, desactiva el color al ejecutar Java con `-Ddatastock.color=false`, o configura la variable de entorno `NO_COLOR=1`.

## 4. Organización del proyecto

```text
DataStock_UT1_Proyecto_Inicial/
├── pom.xml
├── README.md
├── .gitignore
├── datos/
│   ├── ejemplos/              # JSON/XML para comenzar las pruebas
│   └── salidas/               # Exportaciones y binarios generados
├── material_referencia/       # Modelo relacional y script SQL para UT2
└── src/main/java/es/iesisidra/datastock/
    ├── App.java               # Punto de entrada
    ├── modelo/
    │   ├── Producto.java
    │   ├── Categoria.java
    │   ├── Proveedor.java
    │   └── MovimientoStock.java
    ├── servicio/
    │   └── Inventario.java    # Colección compartida en memoria
    ├── ui/
    │   └── MenuPrincipal.java # Menú principal y submenús
    ├── util/
    │   ├── LectorConsola.java
    │   └── ColoresConsola.java
    └── complementarias/
        ├── XPathActividad.java
        └── AccesoAleatorioActividad.java
```

## 5. Menús que ya aparecen

**Menú principal:**

```text
1. Explorar ficheros y directorios     RF-FIC-01
2. Importar productos desde JSON       RF-FIC-02
3. Importar productos desde XML        RF-FIC-03
4. Consultar productos                 RF-FIC-04
5. Exportar productos                  RF-FIC-05
6. Guardar y recuperar inventario      RF-FIC-06
0. Salir
```

**4. Consultar productos:** listar, buscar por código, buscar por nombre, volver.  
**5. Exportar productos:** exportar JSON, exportar XML, volver.  
**6. Guardar y recuperar inventario:** guardar `.dat`, recuperar `.dat`, volver.

`RF-FIC-07` (XPath) y `RF-FIC-08` (`RandomAccessFile`) se ejecutan en clases independientes.

## 6. ¿Qué está hecho y qué debes programar?

**Se proporciona:** clases del modelo con Javadoc y getters/setters, colección inicial vacía, navegación de menús, lectura básica de consola, colores ANSI opcionales, configuración Maven y datos de ejemplo.

**Debes implementar:** la exploración de rutas, lectura/validación/importación JSON y XML, consultas, exportación, serialización y recuperación, XPath y acceso aleatorio. También debes crear las pruebas y registrar sus resultados.

**Importante:** el menú solo debe **presentar opciones y delegar**. No coloques en el `switch` la lógica de ficheros, JSON, XML, validaciones o serialización. Puedes crear tus propios paquetes y clases, por ejemplo `ficheros`, `validacion`, `servicio`.

La clase `Inventario` contiene la lista compartida; el alumnado debe implementar las reglas de alta, búsqueda y sustitución conforme al enunciado. **No se precargan productos:** se empieza con el inventario vacío.

## 7. Reglas clave del enunciado

- JSON raíz `productos` y XML raíz `catalogo` con elementos `producto`.
- Los campos del producto son `codigo`, `nombre`, `precio`, `stock`, `categoriaId` y `proveedorId`.
- Los códigos no se duplican, incluso si cambia el uso de mayúsculas/minúsculas.
- Un documento mal formado o con estructura general inválida **no modifica** el inventario.
- Si el documento es válido pero contiene registros incorrectos, se incorporan solo los válidos.
- Importar JSON/XML **añade** productos; recuperar un `.dat` **sustituye** el inventario, tras confirmar cuando corresponda.
- En UT1 no hay base de datos: los ID de categoría y proveedor se comprueban como enteros positivos, pero no se buscan en MySQL.
- El fichero `stock.bin` de RF-FIC-08 es una **copia independiente**: los cambios hechos allí no actualizan la colección Java.

## 8. GitHub, documentación y pruebas

Crea un repositorio individual, realiza commits con mensajes significativos y mantén actualizado este README con las instrucciones reales de tu solución. Entrega el documento de pruebas cumplimentado con los casos de aceptación del enunciado y conserva ejemplos de entradas válidas e inválidas.

Antes de entregar, comprueba que `mvn clean compile` funciona, que el menú no termina ante entradas inválidas y que puedes explicar el código de cada requisito. La práctica y la prueba individual se evaluarán conforme a la programación didáctica.

## 9. Referencia

Consulta el **enunciado DataStock UT1** facilitado en el aula virtual: es la fuente que determina el comportamiento exigido. El SQL y el diagrama del directorio `material_referencia` se facilitan para entender el proyecto completo; **no hay que conectar MySQL en UT1**.
