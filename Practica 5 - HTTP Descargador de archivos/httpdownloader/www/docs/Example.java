package docs;

/**
 * Clase de ejemplo para pruebas de descarga
 */
public class Example {
    
    private String nombre;
    private int valor;
    
    /**
     * Constructor
     */
    public Example(String nombre, int valor) {
        this.nombre = nombre;
        this.valor = valor;
    }
    
    /**
     * Método de ejemplo
     */
    public void mostrarInfo() {
        System.out.println("Nombre: " + nombre);
        System.out.println("Valor: " + valor);
    }
    
    /**
     * Main de prueba
     */
    public static void main(String[] args) {
        Example ejemplo = new Example("Prueba", 42);
        ejemplo.mostrarInfo();
        System.out.println("¡Descarga exitosa del archivo Java!");
    }
}
