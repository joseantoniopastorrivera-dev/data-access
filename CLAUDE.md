# Data-Access (Acceso a Datos, 2º DAM)

Apuntes en PDF por unidad y la práctica integradora DataStock.

## Estructura
- `UT01-UT1-FICHEROS-Y-FORMATOS-DE-INTERCAMBIO-DE-DATOS/` — apuntes UT1 (PDF).
- `UT02-CONEXIÓN-A-BASES-DE-DATOS-RELACIONALES-(JDBC)/` — apuntes UT2 (PDF).
- `Práctica-integradora-DataStock-v1.0/` — enunciado (`Practica_DataStock_UT1.pdf`), plantilla de pruebas y el proyecto:
  - `DataStock_UT1_Proyecto_Inicial/` — Maven, Java 21, paquete `es.iesisidra`, Gson. Consola. Su `README.md` describe los requisitos; léelo antes de implementar.
  - `datos/ejemplos/` — JSON/XML de prueba (válido, malformado, con errores); `datos/salidas/` — exportaciones.
  - `material_referencia/` — `DataStock_BD_MySQL.sql` y diagrama ER (para la UT2).

## Compilar y ejecutar (desde `DataStock_UT1_Proyecto_Inicial/`)
- `mvn compile` · `mvn exec:java` · consola en UTF-8 para ver tildes.

## Notas
- Práctica individual y evaluable que crece en UT1 (ficheros), UT2 (JDBC) y UT3 (componentes): explica las decisiones para que el alumno pueda defenderlas, y no rompas lo de unidades anteriores.
- Los menús marcados `[PENDIENTE]` son lo que falta por implementar.
- El proyecto aún no está en git (sin seguimiento); `target/` ya está en `.gitignore`.
