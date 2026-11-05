package es.iesisidra.datastock.modelo;

/**
 * Agrupación comercial de productos; corresponde a la tabla {@code categoria}
 * de la futura base de datos de UT2.
 */
public class Categoria {
    private int id;
    private String nombre;

    /** Constructor vacío para facilitar la creación de objetos. */
    public Categoria() { }
    /** @param id identificador de categoría; @param nombre nombre único. */
    public Categoria(int id, String nombre) { this.id = id; this.nombre = nombre; }
    /** @return identificador de categoría. */
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    /** @return nombre de categoría. */
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    @Override public String toString() { return "Categoria{id=" + id + ", nombre='" + nombre + "'}"; }
}
