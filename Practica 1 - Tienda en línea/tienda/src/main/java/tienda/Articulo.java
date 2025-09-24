package tienda;
import org.json.simple.*;
public class Articulo{
    //Atributos
    public String categoria;
    public String nombre;
    public String marca;
    public String descripcion;
    public double precio;
    public int cantidad;

    //constructor
    public Articulo(String categoria, String nombre, String marca, String descripcion, double precio, int cantidad){
        this.categoria = categoria;
        this.nombre = nombre;
        this.marca = marca;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    //constructor vacio
    public Articulo(){
        this.nombre = "";
        this.marca = "";
        this.descripcion = "";
        this.precio = 0.0;
        this.cantidad = 0;
    }

    //metodo para convertir a JSON
    public JSONObject toJSON(){
        JSONObject obj = new JSONObject();
        obj.put("nombre", this.nombre);
        obj.put("marca", this.marca);
        obj.put("descripcion", this.descripcion);
        obj.put("precio", this.precio);
        obj.put("cantidad", this.cantidad);
        obj.put("categoria", this.categoria);
        return obj;
    }
}