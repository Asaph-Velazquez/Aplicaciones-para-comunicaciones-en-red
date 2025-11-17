package ChatGrupal.demo.model;

import java.util.HashSet;
import java.util.Set;

public class Sala {
    private String nombre;
    private Set<String> usuarios;

    public Sala() {
        this.usuarios = new HashSet<>();
    }

    public Sala(String nombre) {
        this.nombre = nombre;
        this.usuarios = new HashSet<>();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Set<String> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(Set<String> usuarios) {
        this.usuarios = usuarios;
    }

    public void agregarUsuario(String usuario) {
        this.usuarios.add(usuario);
    }

    public void removerUsuario(String usuario) {
        this.usuarios.remove(usuario);
    }

    @Override
    public String toString() {
        return "Sala{" +
                "nombre='" + nombre + '\'' +
                ", usuarios=" + usuarios +
                '}';
    }
}
