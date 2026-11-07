package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.validacion.ReglasCampos;
import es.iesisidra.datastock.validacion.ReglasCampos.Campo;
import es.iesisidra.datastock.util.LectorConsola;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pide datos uno a uno y los valida en el momento (apartado 5.1).
 * <p>Regla única para toda la UT2: si el valor no es válido se explica en rojo y
 * se vuelve a pedir; si el usuario pulsa Intro sin escribir nada, la operación
 * se cancela y se vuelve al submenú sin tocar la base de datos.</p>
 */
final class Formulario {
    private Formulario() { }

    /**
     * Pide un dato obligatorio hasta que sea válido.
     * @param etiqueta texto que se muestra (ya indica el campo)
     * @param regla    convierte y valida el texto; devuelve el valor o el motivo del error
     * @return el valor, o vacío si el usuario cancela con Intro
     */
    static <T> Optional<T> pedir(String etiqueta, Function<String, Campo<T>> regla) {
        while (true) {
            String texto = LectorConsola.leerTexto(etiqueta).trim();
            if (texto.isEmpty()) {
                Mensajes.aviso("Operación cancelada. No se ha modificado nada.");
                return Optional.empty();
            }
            Campo<T> campo = regla.apply(texto);
            if (campo.esValido()) {
                return Optional.of(campo.valor());
            }
            Mensajes.error(Mensajes.frase(campo.error()));
        }
    }

    /**
     * En una modificación: Intro conserva el valor actual; si se escribe algo, se valida.
     * @return el valor nuevo, o {@code actual} si se pulsa Intro
     */
    static <T> T pedirOConservar(String etiqueta, T actual, Function<String, Campo<T>> regla) {
        while (true) {
            String texto = LectorConsola.leerTexto(etiqueta + " [" + actual + "] (Intro = conservar): ").trim();
            if (texto.isEmpty()) {
                return actual;
            }
            Campo<T> campo = regla.apply(texto);
            if (campo.esValido()) {
                return campo.valor();
            }
            Mensajes.error(Mensajes.frase(campo.error()));
        }
    }

    // ---- Reglas listas para usar con pedir() ----

    static Campo<Integer> idPositivo(String texto) { return ReglasCampos.enteroPositivo(texto, "el identificador"); }

    static Campo<String> codigoProducto(String texto) {
        String codigo = ReglasCampos.normalizarCodigo(texto);
        String error = ReglasCampos.errorCodigo(codigo);
        return error == null ? Campo.ok(codigo) : Campo.fallo(error);
    }

    static Function<String, Campo<String>> texto(String campo, int minimo, int maximo) {
        return entrada -> {
            String limpio = entrada.trim();
            String error = ReglasCampos.errorTexto(limpio, campo, minimo, maximo);
            return error == null ? Campo.ok(limpio) : Campo.fallo(error);
        };
    }
}
