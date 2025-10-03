package tienda;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.*;


public class Carrito {
    private List<Articulo> productos;
    private double total;
    
    //constructor vacio
    public Carrito(){
        this.productos = new ArrayList<>();
        this.total = 0.0;
    }

    //constructor que recibe un JSON
    public Carrito(String carritoJSON){
        this();
        cargarDesdeJSON(carritoJSON);
    }

    //cargar desde un JSON
    public void cargarDesdeJSON(String carritoJSON){
        try{
            JSONParser parser = new JSONParser();
            JSONArray carritoArray = (JSONArray) parser.parse(carritoJSON);

            for(Object obj: carritoArray){
                JSONObject productoJSON = (JSONObject) obj;
                Articulo articulo = new Articulo(
                    (String) productoJSON.get("categoria"),
                    (String) productoJSON.get("nombre"),
                    (String) productoJSON.get("marca"),
                    (String) productoJSON.get("descripcion"),                    
                    ((Number) productoJSON.get("precio")).doubleValue(),
                    1 
                );
                agregarProducto(articulo);
            }
        }catch(Exception e){
            e.printStackTrace();
            }
    }

    //metodos para el carrito 
    public void agregarProducto(Articulo articulo){
        productos.add(articulo);
        total += articulo.precio;
    }

    public void eliminarProducto(int index){
        if(index >= 0 && index < productos.size()){
            Articulo articulo = productos.remove(index);
            total -= articulo.precio; 
        }
    }

    public void vaciar(){
        productos.clear();
        total = 0.0;
    }

    //Metodos para enviar al servidor
    public void enviarAlServidor(){
        try{
            Socket socket = new Socket("localhost",1234);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);

            //enviar la informacion del carrito al servidor
            JSONObject carritoCompleto = toJSON();
            salida.println("COMPRA:" + carritoCompleto.toJSONString());
            socket.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //convertir a JSON
    @SuppressWarnings("unchecked")
    public JSONObject toJSON(){
        JSONObject carritoJson = new JSONObject();
        JSONArray productosArray = new JSONArray();

        for(Articulo producto: productos){
            productosArray.add(producto.toJSON());
        }
        carritoJson.put("productos", productosArray);
        carritoJson.put("total", total);
        carritoJson.put("cantidad", productos.size());
        return carritoJson;
    }

    //Getters
    public List<Articulo> getProductos(){
        return productos;
    }
    public double getTotal(){
        return total;
    }
    public int getCantidad(){
        return productos.size();
    }

}
