# ***Práctica 2 - Transmisión de MP3 con Go-Back-N***

## 📋 Narrativa
Implementar una aplicación que transmita un archivo MP3 desde un servidor a un cliente usando sockets de datagrama bloqueantes (UDP) y el algoritmo de control de flujo Go-Back-N. El cliente debe reconstruir el archivo recibido y reproducirlo con una interfaz JavaFX que incluya controles (play/pause/stop) y una barra de progreso.

---

## 🛠️ Requisitos del Sistema

### Software necesario
- Java JDK 11+
- Apache Maven 3.6+
- JavaFX (añadir como dependencia para JDK 11+)

### Dependencias (ejemplo en pom.xml)
- org.openjfx:javafx-controls
- org.openjfx:javafx-media

---

## 🚀 Instrucciones de Compilación

### **1. Clonar el repositorio:**
```powershell
git clone https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red.git
cd "Aplicaciones-para-comunicaciones-en-red/Practica 2 - Transmicion de MP3/mp3player"
```

### **2. Compilar con Maven:**
```powershell
# Limpiar y compilar
mvn clean compile

# Crear JAR ejecutable
mvn clean package
```

### **3. Verificar compilación:**
```powershell
# Verificar que se creó el JAR (nombre puede variar según pom.xml)
Get-ChildItem target\*.jar
```

---

## ▶️ Instrucciones de Ejecución

### **✅ Método recomendado (probado y funcional)**

**Paso 1 - Iniciar Servidor:**
1. Abrir el proyecto en VS Code
2. Navegar a `mp3player/src/main/java/backend/Server.java`
3. Click derecho sobre `Server.java` y seleccionar **"Run Java"** (o ejecutar desde Maven/IDE)

El servidor abrirá un socket UDP y esperará solicitudes de envío del archivo MP3.

**Paso 2 - Ejecutar Cliente (interfaz y reproducción):**
1. Abrir `mp3player/src/main/java/backend/Client.java`
2. Click derecho sobre `Client.java` y seleccionar **"Run Java"**
3. La aplicación cliente se conectará (mediante UDP) al servidor para solicitar y recibir el MP3, reconstruirá el archivo y mostrará la interfaz JavaFX para reproducirlo.

### **📋 Orden de Ejecución importante:**

1. 🔴 **PRIMERO:** Iniciar el Servidor (debe estar corriendo)
2. 🟢 **SEGUNDO:** Iniciar el Cliente (se conecta al servidor)

---

## 🌐 Arquitectura de la Aplicación

### **Componentes principales:**

1. **Servidor UDP (`backend/Server.java`):**
   - Lee el archivo MP3 desde disco y lo fragmenta en paquetes.
   - Implementa el algoritmo Go-Back-N para enviar paquetes con ventana deslizante y reenvíos ante timeouts.

2. **Cliente (`backend/Client.java`):**
   - Solicita la transmisión al servidor.
   - Recibe paquetes UDP, detecta pérdida/secuencia y aplica el comportamiento de Go-Back-N (descarta fuera de orden hasta recibir corrección).
   - Reconstruye el MP3 en disco y notifica a la capa de UI cuando hay suficientes datos para reproducir.

3. **Interfaz JavaFX (parte del cliente):**
   - Muestra controles (play/pause/stop), barra de progreso y estado de descarga/reproducción.

4. **Modelo de datos y utilerías:**
   - Clases que representan encabezados de paquete, control de secuencia, y utilerías para fragmentación y ensamblado.

---

## 🔧 Configuración

### **Puertos y parámetros por defecto:**
- **Puerto UDP (servidor):** 5000 (asunción razonable; revisar `Server.java` y `Client.java` si el código usa otro puerto)
- **Tamaño de ventana (Go-Back-N):** configurable en las constantes del servidor/cliente
- **Archivo MP3 de ejemplo:** colocarlo en la carpeta `mp3player/src/main/resources/` o la ruta definida en `Server.java`

### **Personalización:**
- **Cambiar puerto:** Modifica las constantes en `Server.java` y `Client.java`
- **Agregar archivos MP3:** Coloca los archivos en `mp3player/src/main/resources/` o edita la ruta en `Server.java`
- **Ajustar ventana Go-Back-N:** Modifica las constantes de tamaño de ventana y timeout

### **⚠️ Notas importantes:**
- **Ejecutar servidor ANTES que cliente** (orden crítico)
- **Una conexión por servidor** - reiniciar servidor para nueva conexión
- **Puerto UDP** debe estar libre en tu sistema

---

## ✅ Verificar que funciona

- **Servidor:** Debe mostrar en consola que está escuchando en el puerto (por ejemplo, "Servidor UDP iniciado en puerto 5000").
- **Cliente:** Debe iniciar la interfaz JavaFX y, tras completar la descarga o buffer suficiente, reproducir el MP3.

---

## 👥 Colaboradores

- **Desarrollo:** Velazquez Parral Saul Asaph y Amador Martinez Jocelyn Lucia
- **Repositorio:** https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red

---

## 📝 Notas adicionales

- La aplicación usa **sockets UDP** con algoritmo **Go-Back-N** según los requisitos
- El servidor maneja **transmisiones con ventana deslizante** y reenvíos por timeout
- La interfaz JavaFX es **responsiva** y funciona con controles estándar de reproducción
- Los datos se **transmiten fragmentados** con control de secuencia para la comunicación cliente-servidor

---

**🎯 ¡Listo para usar! Si tienes problemas, verifica que Java y Maven estén correctamente instalados.**
