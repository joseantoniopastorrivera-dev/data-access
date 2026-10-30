package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.validacion.DatosProducto;
import es.iesisidra.datastock.validacion.ValidadorProducto;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RF-FIC-06 · Guardar y recuperar el inventario mediante serialización Java.
 * <p>Guarda la colección completa en un fichero binario .dat con
 * ObjectOutputStream y la recupera con ObjectInputStream. A diferencia de
 * JSON y XML, no es un formato de intercambio: solo lo entiende Java y solo
 * con las mismas clases.</p>
 */
public class SerializadorInventario {

    /**
     * Filtro de deserialización: solo se aceptan las clases que DataStock
     * escribe (la lista, los productos y los números que contienen) y con
     * límites de tamaño. Cualquier otra clase se rechaza antes de crearse,
     * lo que evita ataques con ficheros .dat manipulados.
     * <p>java.lang.Object aparece porque ArrayList reserva internamente un
     * Object[] al deserializarse; el patrón solo admite esa clase exacta,
     * no sus subclases.</p>
     */
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "java.util.ArrayList;java.lang.Object;es.iesisidra.datastock.modelo.Producto;"
            + "java.math.BigDecimal;java.math.BigInteger;java.lang.Number;"
            + "maxdepth=10;maxrefs=100000;maxarray=100000;!*");

    private final ValidadorProducto validador = new ValidadorProducto();

    /**
     * Serializa la colección completa en el fichero de destino.
     * @param productos productos que se guardan
     * @param destino   fichero .dat (su directorio ya se ha comprobado)
     * @throws ExportacionException si no se puede escribir; un fichero anterior no se modifica
     */
    public void guardar(List<Producto> productos, Path destino) throws ExportacionException {
        // Se guarda siempre un ArrayList: es la clase que el filtro admite al recuperar.
        ArrayList<Producto> copia = new ArrayList<>(productos);
        EscrituraAtomica.escribir(destino, temporal -> {
            try (ObjectOutputStream salida = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporal)))) {
                salida.writeObject(copia);
            }
        });
    }

    /**
     * Lee y valida un inventario serializado. No modifica nada: quien llama
     * decide si sustituye el inventario con la lista devuelta.
     * @param ruta fichero .dat generado por DataStock
     * @return los productos recuperados, todos válidos y sin códigos repetidos
     * @throws ImportacionException si el fichero está dañado, no es de DataStock o contiene datos inválidos
     */
    public List<Producto> recuperar(Path ruta) throws ImportacionException {
        Object leido;
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(ruta)))) {
            entrada.setObjectInputFilter(FILTRO);
            leido = entrada.readObject();
        } catch (InvalidClassException ex) {
            throw new ImportacionException("El fichero contiene clases no permitidas o de una versión "
                    + "incompatible: no es un inventario guardado por DataStock.", ex);
        } catch (StreamCorruptedException | EOFException ex) {
            throw new ImportacionException("El fichero está dañado o no es un inventario serializado de DataStock.", ex);
        } catch (ClassNotFoundException ex) {
            throw new ImportacionException("El fichero contiene clases desconocidas: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ImportacionException("No se pudo leer el fichero: " + ex.getMessage(), ex);
        }
        return validarContenido(leido);
    }

    /** Comprueba que lo leído es una lista de productos válidos y sin duplicados. */
    private List<Producto> validarContenido(Object leido) throws ImportacionException {
        if (!(leido instanceof List<?> lista)) {
            throw new ImportacionException("El fichero no contiene una lista de productos.");
        }
        List<Producto> productos = new ArrayList<>();
        Set<String> codigos = new HashSet<>();
        int posicion = 0;
        for (Object elemento : lista) {
            posicion++;
            if (!(elemento instanceof Producto p)) {
                throw new ImportacionException("El elemento " + posicion + " no es un producto.");
            }
            List<String> errores = new ArrayList<>();
            Producto validado = validador.validar(aDatos(posicion, p), errores);
            if (validado == null) {
                throw new ImportacionException("Producto " + posicion + " no válido: " + String.join("; ", errores));
            }
            if (!codigos.add(validado.getCodigo())) {
                throw new ImportacionException("El código " + validado.getCodigo() + " está repetido en el fichero.");
            }
            productos.add(validado);
        }
        return productos;
    }

    /**
     * Pasa el producto leído por las mismas reglas que una importación.
     * Un .dat manipulado podría contener, por ejemplo, un precio negativo.
     * Se usa toString() y no toPlainString(): con un precio enorme manipulado,
     * toPlainString() agotaría la memoria antes de poder validarlo.
     */
    private DatosProducto aDatos(int posicion, Producto p) {
        return new DatosProducto(posicion, p.getCodigo(), p.getNombre(),
                p.getPrecio() == null ? null : p.getPrecio().toString(),
                String.valueOf(p.getStock()), String.valueOf(p.getCategoriaId()), String.valueOf(p.getProveedorId()));
    }
}
