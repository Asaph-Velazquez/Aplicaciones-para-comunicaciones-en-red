# ***Práctica 1 - Tienda en línea***

## 📋 Narrativa
Implementar una aplicación para la venta de artículos en línea mediante el API de sockets de flujo bloqueante.
Cada usuario contará con un menú (línea de comandos o GUI) desde donde podrá:
- Buscar artículos por nombre o marca 
- Listar artículos por tipo
- Agregar artículos al carrito de compra
- Editar contenido del carrito de compra
- Finalizar compra y obtener ticket

***Validar existencias antes de agregar los artículos al carrito y decrementar existencias***

## 🛠️ Requisitos del Sistema

### **Software necesario:**
- ☕ **Java JDK 8+** (recomendado JDK 11 o superior)
- 📦 **Apache Maven 3.6+**
- 🖥️ **JavaFX** (incluido en JDK 8, separado en JDK 11+)
- 🔧 **IDE recomendado:** VS Code

### **Dependencias del proyecto:**
- `org.json.simple` - Para manejo de JSON
- `javafx` - Para interfaz gráfica WebView

## 🚀 Instrucciones de Compilación

### **1. Clonar el repositorio:**
```bash
git clone https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red.git
cd "Aplicaciones-para-comunicaciones-en-red/Practica 1 - Tienda en línea/tienda"
```

### **2. Compilar con Maven:**
```bash
# Limpiar y compilar
mvn clean compile

# Crear JAR ejecutable
mvn clean package
```

### **3. Verificar compilación:**
```bash
# Verificar que se creó el JAR
ls target/tienda-1.0-SNAPSHOT.jar
```

## ▶️ Instrucciones de Ejecución

### **✅ Método Recomendado (Probado y Funcional)**

**Paso 1 - Iniciar Servidor en VS Code:**
1. Abrir VS Code en la carpeta del proyecto
2. Navegar a `src/main/java/tienda/Servidor.java`
3. **Click derecho** sobre el archivo `Servidor.java`
4. Seleccionar **"Run Java"**
5. ✅ El servidor iniciará en el puerto 1234

**Paso 2 - Ejecutar Cliente desde Terminal:**
```bash
# En la terminal (dentro de la carpeta tienda/)
mvn clean compile exec:java
```

### **📋 Orden de Ejecución Importante:**

1. **🔴 PRIMERO:** Iniciar Servidor (debe estar corriendo)
2. **🟢 SEGUNDO:** Iniciar Cliente (se conecta al servidor)


### **✅ Verificar que funciona:**

- **Servidor:** Debe mostrar "Servidor iniciado" en consola
- **Cliente:** Debe abrir ventana JavaFX con productos del servidor  


## 🌐 Arquitectura de la Aplicación

### **Componentes principales:**

1. **🖥️ Servidor TCP (`Servidor.java`):**
   - Escucha en puerto `1234`
   - Envía productos en formato JSON
   - Maneja múltiples conexiones secuenciales

2. **📱 Cliente Catálogo (`Cliente.java`):**
   - Interfaz web con JavaFX WebView
   - Muestra productos en grid responsivo
   - Búsqueda y filtrado en tiempo real
   - Tema visual tipo madera

3. **🛒 Carrito de Compras (`CarritoGUI.java`):**
   - Interfaz separada para gestión de carrito
   - Conecta con el servidor para obtener productos
   - Persistencia temporal con localStorage

4. **📦 Modelo de Datos (`Articulo.java`, `Carrito.java`):**
   - Clases POJO para productos y carrito
   - Serialización JSON
   - Validaciones de negocio

## 🔧 Configuración

### **Puertos utilizados:**
- **Servidor:** `localhost:1234`
- **Cliente web:** `file:///.../index.html`
- **Carrito web:** `file:///.../carrito.html`

### **Personalización:**
- **Cambiar puerto:** Modifica `1234` en `Servidor.java` y `Cliente.java`
- **Agregar productos:** Edita los objetos `Articulo` en `Servidor.java`
- **Modificar UI:** Actualiza archivos HTML/CSS en `src/main/resources/ui/`

### **⚠️ Notas importantes:**
- **Ejecutar servidor ANTES que cliente** (orden importante)
- **Una conexión por servidor** - reiniciar servidor para nueva conexión
- **Puerto 1234** debe estar libre en tu sistema

## 👥 Colaboradores

- **Desarrollo:** Velazquez Parral Saul Asaph y Amador Martinez Jocelyn Lucia
- **Repositorio:** https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red

## 📝 Notas Adicionales

- La aplicación usa **sockets TCP bloqueantes** según los requisitos
- El servidor maneja **conexiones secuenciales** (una a la vez)
- La interfaz web es **responsive** y funciona en navegadores modernos
- Los datos se **serializan en JSON** para la comunicación cliente-servidor

---

**🎯 ¡Listo para usar! Si tienes problemas, verifica que Java y Maven estén correctamente instalados.** 
