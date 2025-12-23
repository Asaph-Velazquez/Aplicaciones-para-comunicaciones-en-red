# ***Práctica 5 - HTTP Descargador de archivos***

## 📋 Narrativa
Implementar un servidor HTTP básico que funcione como descargador de archivos con capacidades de descarga recursiva similar a `wget -r`.

El servidor proporciona:
- **Descarga recursiva desde servidores remotos** - Descarga archivos y sus enlaces (comportamiento tipo wget -r)
- **Descarga desde directorio local** - Sirve archivos del sistema de archivos local
- **Interfaz web** - UI moderna para interactuar con el servidor
- **Soporte múltiples tipos MIME** - HTML, CSS, JS, PDF, imágenes, documentos, etc.
- **Manejo concurrente** - Múltiples clientes simultáneos mediante pool de hilos

***Implementación con sockets puros (ServerSocket/Socket) y parsing manual de HTTP/1.1***

## 🛠️ Requisitos del Sistema

### **Software necesario:**
- ☕ **Java JDK 17+** (recomendado)
- 📦 **Apache Maven 3.6+**
- 🔧 **IDE recomendado:** VS Code
- 🌐 **Navegador web moderno** (Chrome, Firefox, Edge)

## 🚀 Instrucciones de Compilación

### **1. Clonar el repositorio:**
```bash
git clone https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red.git
cd "Aplicaciones-para-comunicaciones-en-red/Practica 5 - HTTP Descargador de archivos/httpdownloader"
```

### **2. Compilar con Maven:**
```bash
# Limpiar y compilar
mvn clean compile

# Crear JAR ejecutable (opcional)
mvn clean package
```

### **3. Verificar compilación:**
```bash
# Verificar que las clases fueron compiladas
ls target/classes/backend/
```

## ▶️ Instrucciones de Ejecución

### **✅ Método 1: Usando el script run.bat (Windows)**

```bash
# Ejecutar directamente desde el directorio httpdownloader/
.\run.bat
```

El script automáticamente:
- ✅ Verifica que el proyecto esté compilado
- ✅ Inicia el servidor en puerto 8080
- ✅ Muestra la URL de acceso

### **✅ Método 2: Con Maven**

```bash
# Compilar y ejecutar en un solo comando
mvn clean compile exec:java -Dexec.mainClass="backend.Main"
```

### **✅ Método 3: Desde VS Code**

1. Abrir VS Code en la carpeta del proyecto
2. Navegar a `src/main/java/backend/Main.java`
3. **Click derecho** sobre el archivo `Main.java`
4. Seleccionar **"Run Java"**
5. ✅ El servidor iniciará en el puerto 8080

### **✅ Verificar que funciona:**

**Consola del servidor debe mostrar:**
```
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║     🌐  SERVIDOR HTTP - JAVA SE PURO                     ║
║                                                           ║
║     📚 Práctica 5: Descargador de Archivos               ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝

✅ Servidor HTTP iniciado en el puerto 8080
🌐 Accede a: http://localhost:8080
```

**Acceder desde navegador:**
```
http://localhost:8080
```

## 🌐 Arquitectura de la Aplicación

### **Componentes principales:**

1. **🖥️ HTTPServer (`HTTPServer.java`):**
   - Servidor basado en `ServerSocket`
   - Escucha en puerto `8080`
   - Pool de hilos con `ExecutorService` para concurrencia
   - Acepta múltiples conexiones simultáneas

2. **👥 ClientHandler (`ClientHandler.java`):**
   - Maneja cada conexión de cliente en un hilo separado
   - Parsea peticiones HTTP/1.1 manualmente
   - Enruta a los controladores correspondientes
   - Envía respuestas HTTP formateadas

3. **🌍 RemoteDownloader (`RemoteDownloader.java`):**
   - Actúa como cliente HTTP usando sockets
   - Descarga recursiva de archivos remotos
   - Comportamiento similar a `wget -r`
   - Parsea HTML para extraer enlaces
   - Manejo concurrente de descargas

4. **📂 LocalFileServer (`LocalFileServer.java`):**
   - Sirve archivos del sistema de archivos local
   - Soporte para múltiples tipos MIME
   - Manejo de rutas y seguridad básica
   - Generación de listados de directorios

5. **📝 HTTPRequest/Response (`HTTPRequest.java`, `HTTPResponse.java`):**
   - Parsing manual de HTTP/1.1
   - Construcción de respuestas HTTP válidas
   - Manejo de headers y códigos de estado

6. **🔍 HTMLParser (`HTMLParser.java`):**
   - Extrae enlaces de documentos HTML
   - Normaliza URLs
   - Convierte URLs relativas a absolutas

## 🎯 Funcionalidades

### **1. Descarga Recursiva desde Internet:**
```
http://localhost:8080/download/net?url=http://ejemplo.com
```
- Descarga el archivo/página especificado
- Extrae todos los enlaces HTML
- Descarga recursivamente los archivos enlazados
- Guarda en carpeta `downloads/`

### **2. Descarga desde Directorio Local:**
```
http://localhost:8080/download/local?path=/docs
```
- Sirve archivos del directorio `www/`
- Soporta múltiples tipos de archivos
- Genera listados de directorios

### **3. Interfaz Web:**
```
http://localhost:8080/
```
- UI moderna con diseño responsive
- Formularios para descarga remota y local
- Visualización de resultados en tiempo real
- Tema oscuro con acentos dorados

## 🔧 Configuración

### **Puertos utilizados:**
- **Servidor HTTP:** `localhost:8080`

### **Directorios importantes:**
- **www/** - Archivos estáticos (HTML, CSS, JS)
- **downloads/** - Archivos descargados desde Internet
- **www/docs/** - Archivos locales disponibles para descarga

### **Personalización:**

**Cambiar puerto del servidor:**
```java
// En Main.java
private static final int PORT = 8080; // Cambiar aquí
```

**Ajustar pool de hilos:**
```java
// En Main.java
private static final int THREAD_POOL_SIZE = 10; // Número de hilos concurrentes
```

**Modificar profundidad de descarga recursiva:**
```java
// En ClientHandler.java, método handleNetDownload()
RemoteDownloader downloader = new RemoteDownloader(5, 2, outputDir);
//                                                     ^  ^ maxDepth
//                                                     maxThreads
```

## 📡 Endpoints Disponibles

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/` | GET | Interfaz web principal |
| `/download/net` | GET | Descarga recursiva desde URL remota |
| `/download/local` | GET | Descarga desde directorio local |
| `/interface.html` | GET | Página de interfaz |
| `/result.html` | GET | Página de resultados |
| `/*` | GET | Archivos estáticos (CSS, JS, imágenes) |

## 🔍 Tipos MIME Soportados

El servidor reconoce y maneja correctamente:
- **HTML** - text/html
- **CSS** - text/css
- **JavaScript** - application/javascript
- **JSON** - application/json
- **PDF** - application/pdf
- **Imágenes** - image/png, image/jpeg, image/gif, image/svg+xml
- **Documentos** - application/msword, application/vnd.ms-excel, etc.
- **Texto** - text/plain
- **Binarios** - application/octet-stream (por defecto)

## 👥 Colaboradores

- **Desarrollo:** Velazquez Parral Saul Asaph y Amador Martinez Jocelyn Lucia
- **Repositorio:** https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red

## 📝 Ejemplos de Uso

### **Descarga desde servidor remoto:**
1. Abrir navegador en `http://localhost:8080`
2. En el formulario "Descarga desde Internet"
3. Ingresar URL: `http://example.com`
4. Hacer clic en "Descargar"
5. Los archivos se guardan en `downloads/`

### **Descarga desde directorio local:**
1. Colocar archivos en `www/docs/`
2. Acceder a `http://localhost:8080/download/local?path=/docs`
3. Navegar por el listado de archivos
4. Hacer clic en archivos para descargar

### **Servir archivos estáticos:**
1. Agregar archivos a `www/`
2. Acceder a `http://localhost:8080/nombrearchivo.ext`

## 🐛 Solución de Problemas

**Puerto 8080 en uso:**
```
Error: Address already in use
Solución: Cambiar puerto en Main.java o cerrar proceso usando el puerto
```

**No se ve la interfaz:**
```
Verificar que existe www/interface.html
Verificar permisos de lectura en directorio www/
```

**Descarga recursiva no funciona:**
```
Verificar conectividad a Internet
Verificar que la URL es HTTP (no HTTPS)
Revisar logs del servidor para errores
```

**Archivos no se descargan:**
```
Verificar permisos de escritura en carpeta downloads/
Verificar espacio en disco
Revisar logs del servidor
```

---

**🎯 ¡Servidor HTTP listo! Accede a http://localhost:8080 para comenzar a descargar archivos.**
