package tienda;
import java.util.List;
import java.util.ArrayList;

public class Carrito {
    private List<Articulo> articulos;
    private double total;
    
    public Carrito() {
        this.articulos = new ArrayList<>();
        this.total = 0.0;
    }

    public void agregarArticulo(Articulo articulo) {
        if (articulo == null) {
            throw new IllegalArgumentException("El artículo no puede ser null");
        }
        this.articulos.add(articulo);
        this.total += articulo.precio;
    }
    
    public boolean removerArticulo(Articulo articulo) {
        if (this.articulos.remove(articulo)) {
            this.total -= articulo.precio;
            return true;
        }
        return false;
    }
    
    public double getTotal() {
        return total;
    }
    
    public List<Articulo> getArticulos() {
        return new ArrayList<>(articulos);
    }
    
    public void vaciarCarrito() {
        this.articulos.clear();
        this.total = 0.0;
    }
    
    public int getCantidadItems() {
        return articulos.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Carrito de Compras:\n");
        for (int i = 0; i < articulos.size(); i++) {
            sb.append((i + 1)).append(". ").append(articulos.get(i).toString()).append("\n");
        }
        sb.append("Total: $").append(String.format("%.2f", total));
        return sb.toString();
    }
}
