# 🗄️ Data-Access (Acceso a Datos)

Bienvenido/a al repositorio de la asignatura de **Acceso a Datos**, correspondiente al segundo año del Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM). 

En este espacio se almacenan los apuntes teóricos, manuales y las prácticas desarrolladas durante el curso, enfocadas en la persistencia de información, el manejo de ficheros y la integración con bases de datos.

## 📂 Estructura del Repositorio

El repositorio está organizado por Unidades de Trabajo (UT) y prácticas integradoras:

* **`ut01-ut1-ficheros-y-formatos-de-intercambio-de-datos/`**
  * Apuntes de la unidad (`apuntes-acceso-a-datos-ut1-ficheros.pdf`) y el anexo de la clase `RandomAccessFile`.
  * Una carpeta por apartado con su teoría y sus ejercicios: `1-1-ficheros/` (NIO, File, RandomAccessFile y bytes, con ejemplos en `ejemplos/`), `1-2-serializacion-deserializacion-objetos/`, `1-3-intercambio-de-datos/`, `1-4-analizadores/` y `1-5-consultas-xpath/`.
* **`ut02-conexion-a-bases-de-datos-relacionales-jdbc/`**
  * Documentación completa sobre JDBC. Incluye conceptos básicos, ejecución de sentencias DML y DDL, procesamiento de resultados, gestión de transacciones y la guía didáctica de la unidad.
* **`practica-integradora-datastock-v1-0/`**
  * Proyecto práctico de la UT1 llamado **DataStock**. Incluye el código fuente (versión 1.0), el enunciado de la práctica, los archivos comprimidos de entrega y la plantilla de registro de pruebas (Testing).
* **`practica-integradora-datastock-v2-0/`**
  * Entrega de la UT2: DataStock ampliado con acceso a base de datos mediante JDBC. Proyecto Maven (`pom.xml`) con `src/`, `config/`, `datos/`, `docs/` y su propio `README.md`, más el enunciado (`practica-datastock-ut2.pdf`), el comprimido de entrega (`DataStock_UT2_JoseAntonio_PastorRivera_V1.0.zip`) y el registro de pruebas en PDF.
* **`practica-integradora-datastock-v3-0/`**
  * Entrega de la UT3, que parte de la versión de la UT2 y extrae dos componentes reutilizables: `LectorProductosJson` (opción 2 del menú) y `ConsultaProductoJdbc` (opción 7.5.2). Incluye pruebas con JUnit 5, el registro de pruebas con sus evidencias y el enunciado (`practica-datastock-ut3.pdf`).

Cada versión de DataStock es un proyecto independiente con su propio `README.md`, donde se explica cómo compilarlo y ejecutarlo.

## 💻 Tecnologías y Conceptos
* **Lenguaje principal:** Java, con Maven y JUnit 5 en las prácticas integradoras.
* **Persistencia:** Manejo de Ficheros de texto/binarios, Formatos de intercambio.
* **Bases de Datos:** Conectores JDBC, SQL (DDL, DML), Control de Transacciones.

## 👨‍💻 Autor
* **José Antonio Pastor Rivera** - *Desarrollo y documentación*
