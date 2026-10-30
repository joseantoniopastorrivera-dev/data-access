package es.iesisidra.datastock.modelo;

/**
 * Empresa suministradora de artículos. Su correo electrónico es opcional.
 * Se corresponde con la tabla {@code proveedor} de la fase JDBC.
 */
public class Proveedor {
    private int id;
    private String nombre;
    private String email;

    /** Constructor sin argumentos. */
    public Proveedor() { }
    /** Crea un proveedor con identificador, nombre y correo opcional. */
    public Proveedor(int id, String nombre, String email) {
        this.id = id; this.nombre = nombre; this.email = email;
    }
    /** @return identificador de proveedor. */
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    /** @return nombre de proveedor. */
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    /** @return correo electrónico, o null si no se informa. */
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @Override public String toString() { return "Proveedor{id=" + id + ", nombre='" + nombre + "', email='" + email + "'}"; }
}
