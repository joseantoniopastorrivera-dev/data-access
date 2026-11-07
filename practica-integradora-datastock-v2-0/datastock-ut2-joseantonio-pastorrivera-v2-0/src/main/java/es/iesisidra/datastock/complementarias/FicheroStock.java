package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.modelo.Producto;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RF-FIC-08 · Fichero binario de registros de longitud fija con RandomAccessFile.
 * <p>Formato de cada registro (36 bytes):</p>
 * <pre>
 *  bytes 0-23  código: 12 caracteres Java (writeChar = 2 bytes cada uno), relleno con espacios
 *  bytes 24-31 precio: double (8 bytes)
 *  bytes 32-35 stock:  int (4 bytes)
 * </pre>
 * <p>Inicio del registro = posición × 36; campo stock = posición × 36 + 32.
 * El fichero es una copia independiente: modificarlo no cambia los objetos
 * Producto en memoria, ni al revés.</p>
 */
public class FicheroStock {
    public static final int LONGITUD_CODIGO = 12;
    public static final int TAM_REGISTRO = LONGITUD_CODIGO * Character.BYTES + Double.BYTES + Integer.BYTES; // 36
    private static final int DESPLAZAMIENTO_STOCK = LONGITUD_CODIGO * Character.BYTES + Double.BYTES;         // 32

    /** Un registro leído del fichero. */
    public record RegistroStock(int posicion, String codigo, double precio, int stock) { }

    private final Path ruta;

    public FicheroStock(Path ruta) {
        this.ruta = ruta;
    }

    public Path getRuta() {
        return ruta;
    }

    /**
     * Crea (o vacía y vuelve a crear) el fichero con una copia de los productos, ordenados por código.
     * @return número de registros escritos
     */
    public int generar(List<Producto> productos) throws IOException {
        List<Producto> ordenados = new ArrayList<>(productos);
        ordenados.sort(Comparator.comparing(Producto::getCodigo));
        try (RandomAccessFile fichero = new RandomAccessFile(ruta.toFile(), "rw")) {
            fichero.setLength(0);   // trunca: si el fichero anterior era más largo, no quedan restos
            for (Producto p : ordenados) {
                escribirCodigo(fichero, p.getCodigo());
                fichero.writeDouble(p.getPrecio().doubleValue());
                fichero.writeInt(p.getStock());
            }
        }
        return ordenados.size();
    }

    /**
     * Comprueba que el fichero existe y que su tamaño es múltiplo de 36.
     * @return número de registros
     * @throws IOException si no existe o su tamaño no corresponde a registros completos
     */
    public int numeroRegistros() throws IOException {
        if (!Files.isRegularFile(ruta)) {
            throw new IOException("El fichero " + ruta + " no existe. Genéralo primero.");
        }
        long tamano = Files.size(ruta);
        if (tamano % TAM_REGISTRO != 0) {
            throw new IOException("Fichero inválido: ocupa " + tamano + " bytes, que no es múltiplo de "
                    + TAM_REGISTRO + ".");
        }
        return (int) (tamano / TAM_REGISTRO);
    }

    /** Acceso directo: salta al registro con seek(posición × 36) sin leer los anteriores. */
    public RegistroStock leer(int posicion) throws IOException {
        comprobarPosicion(posicion);
        try (RandomAccessFile fichero = new RandomAccessFile(ruta.toFile(), "r")) {
            fichero.seek((long) posicion * TAM_REGISTRO);
            return leerRegistro(fichero, posicion);
        }
    }

    /**
     * Modifica solo el campo stock con seek(posición × 36 + 32), sin reescribir el registro.
     * @return el stock que había antes del cambio
     */
    public int modificarStock(int posicion, int nuevoStock) throws IOException {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        comprobarPosicion(posicion);
        try (RandomAccessFile fichero = new RandomAccessFile(ruta.toFile(), "rw")) {
            long posicionStock = (long) posicion * TAM_REGISTRO + DESPLAZAMIENTO_STOCK;
            fichero.seek(posicionStock);
            int anterior = fichero.readInt();
            fichero.seek(posicionStock);   // readInt ha avanzado el puntero 4 bytes: hay que volver
            fichero.writeInt(nuevoStock);
            return anterior;
        }
    }

    /** Lectura secuencial de todos los registros, desde el principio. */
    public List<RegistroStock> leerTodos() throws IOException {
        int total = numeroRegistros();
        List<RegistroStock> registros = new ArrayList<>();
        try (RandomAccessFile fichero = new RandomAccessFile(ruta.toFile(), "r")) {
            for (int i = 0; i < total; i++) {
                registros.add(leerRegistro(fichero, i));
            }
        }
        return registros;
    }

    private void comprobarPosicion(int posicion) throws IOException {
        int total = numeroRegistros();
        if (posicion < 0 || posicion >= total) {
            throw new IllegalArgumentException("Posición fuera de rango: debe estar entre 0 y " + (total - 1) + ".");
        }
    }

    /** Escribe exactamente 12 caracteres: recorta si sobra y rellena con espacios si falta. */
    private void escribirCodigo(RandomAccessFile fichero, String codigo) throws IOException {
        StringBuilder texto = new StringBuilder(codigo.length() > LONGITUD_CODIGO
                ? codigo.substring(0, LONGITUD_CODIGO) : codigo);
        while (texto.length() < LONGITUD_CODIGO) {
            texto.append(' ');
        }
        fichero.writeChars(texto.toString());
    }

    /** Lee un registro desde la posición actual del puntero. */
    private RegistroStock leerRegistro(RandomAccessFile fichero, int posicion) throws IOException {
        char[] codigo = new char[LONGITUD_CODIGO];
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            codigo[i] = fichero.readChar();
        }
        double precio = fichero.readDouble();
        int stock = fichero.readInt();
        return new RegistroStock(posicion, new String(codigo).trim(), precio, stock);
    }
}
