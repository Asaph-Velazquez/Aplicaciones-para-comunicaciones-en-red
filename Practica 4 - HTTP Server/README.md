# Servidor HTTP Simple - Java SE

## Descripción

Servidor HTTP implementado completamente en **Java SE puro** usando **sockets** (`ServerSocket` y `Socket`), sin frameworks ni librerías HTTP externas. El servidor implementa manualmente el protocolo **HTTP/1.1** e incluye un sistema de concurrencia con pool de hilos y redirección automática.

## 🎯 Características Principales

### Protocolo HTTP/1.1 Manual
- ✅ Parsing manual de request line (método, recurso, versión)
- ✅ Lectura y manejo de headers
- ✅ Manejo de body usando `Content-Length`
- ✅ Construcción manual de respuestas HTTP bien formadas

### Métodos HTTP Soportados
- **GET**: Sirve archivos estáticos
- **POST**: Acepta datos en el body
- **PUT**: Acepta actualizaciones
- **DELETE**: Procesa eliminaciones
- **HEAD**: Retorna headers sin body
- **TRACE**: Devuelve la request para diagnóstico

### Tipos MIME
El servidor detecta y envía correctamente los siguientes tipos MIME:
- `text/html` - Páginas HTML
- `text/plain` - Archivos de texto
- `application/json` - Datos JSON
- `image/png` - Imágenes PNG
- `image/jpeg` - Imágenes JPEG
- `text/css` - Hojas de estilo
- `application/javascript` - JavaScript
- `application/pdf` - Documentos PDF

### Sistema de Concurrencia
- Pool de hilos manual usando `ExecutorService`
- Contador de conexiones activas en tiempo real
- Monitoreo de carga del servidor

### Redirección Automática
Cuando las conexiones activas superan el **50% del pool**:
1. Se levanta automáticamente un servidor secundario en el puerto 8081
2. Nuevas peticiones se redirigen con código **HTTP 307 (Temporary Redirect)**
3. Se utiliza el header `Location` para indicar el servidor secundario

### Interfaz Web
- HTML + CSS + JavaScript para navegación de archivos
- Explorador de archivos estáticos
- Panel de pruebas de métodos HTTP
- Información del servidor en tiempo real

## 📁 Estructura del Proyecto

```
http_server/
├── pom.xml                          # Configuración Maven
├── README.md                        # Este archivo
├── src/
│   └── main/
│       └── java/
│           └── http_server/
│               ├── Main.java                  # Punto de entrada
│               ├── HttpServer.java            # Servidor HTTP principal
│               ├── HttpRequest.java           # Modelo de request
│               ├── HttpResponse.java          # Modelo de response
│               ├── HttpRequestParser.java     # Parser de HTTP
│               ├── RequestHandler.java        # Manejador de requests
│               └── RedirectionManager.java    # Gestor de redirección
└── www/                             # Archivos estáticos
    ├── index.html                   # Interfaz web principal
    ├── styles.css                   # Estilos
    ├── app.js                       # JavaScript
    ├── test.json                    # Archivo JSON de prueba
    ├── sample.txt                   # Archivo de texto de prueba
    └── uploads/                     # 📤 Directorio para tus archivos
        ├── INSTRUCCIONES.md         # Guía de uso de uploads/
        ├── prueba.html              # HTML de ejemplo
        ├── ejemplo.json             # JSON de ejemplo
        └── README.txt               # Instrucciones rápidas
```

## 🔧 Configuración

### Requisitos
- **Java 8** o superior
- **Maven** (opcional, para compilar)

### Parámetros del Servidor
En [`Main.java`](src/main/java/http_server/Main.java):
```java
private static final int PRIMARY_PORT = 8080;    // Puerto primario
private static final int SECONDARY_PORT = 8081;  // Puerto secundario
private static final int POOL_SIZE = 10;         // Tamaño del pool de hilos
```

## 🚀 Uso

### Compilar el Proyecto

Con Maven:
```bash
mvn clean compile
```

Con javac (alternativa):
```bash
javac -d target/classes src/main/java/http_server/*.java
```

### Ejecutar el Servidor

Con Maven:
```bash
mvn exec:java -Dexec.mainClass="http_server.Main"
```

Con java (alternativa):
```bash
java -cp target/classes http_server.Main
```

### Acceder a la Interfaz Web
Abre tu navegador en:
```
http://localhost:8080
```

### Probar el Servidor

#### Con curl:
```bash
# GET
curl http://localhost:8080/index.html

# POST con body
curl -X POST http://localhost:8080/test.json -d '{"key":"value"}'

# PUT
curl -X PUT http://localhost:8080/resource -d 'data'

# DELETE
curl -X DELETE http://localhost:8080/resource

# HEAD
curl -I http://localhost:8080/index.html

# TRACE
curl -X TRACE http://localhost:8080/
```

#### Con la interfaz web:
1. Navega a `http://localhost:8080`
2. Usa el panel "Probar Métodos HTTP"
3. Selecciona un método y haz clic en el botón correspondiente

## 🧪 Probar la Redirección

Para activar el servidor secundario y ver la redirección en acción:

1. Abre **10 o más conexiones simultáneas** (usando curl en bucle o un script)
2. El servidor detectará más del 50% de carga
3. Se levantará automáticamente el servidor secundario en puerto 8081
4. Las nuevas peticiones recibirán un **HTTP 307** redirigiendo a `http://localhost:8081`

Ejemplo con bash (Linux/Mac):
```bash
for i in {1..15}; do
  curl http://localhost:8080/sample.txt &
done
```

Ejemplo con PowerShell (Windows):
```powershell
1..15 | ForEach-Object {
  Start-Job { curl http://localhost:8080/sample.txt }
}
```

## 📚 Explicación del Código

### 1. HttpRequest y HttpResponse
Clases de modelo que representan las peticiones y respuestas HTTP.

### 2. HttpRequestParser
Parsea manualmente las peticiones HTTP:
- Lee la **request line** y extrae método, recurso y versión
- Lee los **headers** línea por línea hasta encontrar línea vacía
- Lee el **body** usando el header `Content-Length`

### 3. RequestHandler
Procesa las peticiones según el método HTTP:
- `GET`: Lee y sirve archivos desde `www/`
- `POST/PUT`: Acepta body y retorna confirmación
- `DELETE`: Procesa eliminación
- `HEAD`: Retorna solo headers
- `TRACE`: Devuelve la request completa

### 4. HttpServer
Servidor principal que:
- Crea un `ServerSocket` en el puerto especificado
- Mantiene un pool de hilos con `ExecutorService`
- Acepta conexiones en un loop infinito
- Delega cada conexión a un hilo del pool
- Monitorea conexiones activas con `AtomicInteger`

### 5. RedirectionManager
Gestiona el servidor secundario:
- Detecta cuando la carga supera el 50%
- Levanta automáticamente el servidor secundario
- Proporciona información para construir respuestas 307

### 6. Main
Punto de entrada que:
- Configura los puertos y pool size
- Crea el `RedirectionManager`
- Inicia el servidor primario
- Configura shutdown hook para cierre graceful

## 🎓 Conceptos Académicos Demostrados

### Redes y Sockets
- Uso de `ServerSocket` para escuchar conexiones
- Uso de `Socket` para comunicación bidireccional
- Lectura de `InputStream` y escritura en `OutputStream`

### Protocolo HTTP
- Parsing manual de request line: `METHOD /resource HTTP/1.1`
- Lectura de headers en formato `Key: Value`
- Separación headers-body con `\r\n\r\n`
- Construcción de respuestas: status line + headers + body

### Concurrencia
- Pool de hilos con `ExecutorService.newFixedThreadPool()`
- Variables atómicas con `AtomicInteger` para thread-safety
- Sincronización con `synchronized` en `RedirectionManager`

### Patrones de Diseño
- **Separación de responsabilidades**: cada clase tiene un propósito claro
- **Encapsulación**: datos privados con getters/setters
- **Factory methods**: métodos estáticos para crear respuestas comunes

## 🔒 Seguridad

El servidor implementa:
- Validación de paths para evitar **directory traversal**
- Verificación de que los archivos están dentro de `www/`
- Manejo de excepciones para evitar crashes

**⚠️ IMPORTANTE**: Este es un servidor educativo. **NO usarlo en producción**.

## 📝 Notas Importantes

- **Sin librerías externas**: Todo implementado con Java SE puro
- **Código simple y claro**: Prioriza legibilidad sobre optimización
- **Bien comentado**: Cada clase y método tiene documentación
- **Defendible oralmente**: Cada decisión tiene justificación clara

## 🐛 Limitaciones Conocidas

- No soporta HTTP/2
- No implementa HTTPS/TLS
- No maneja chunked transfer encoding
- No implementa keep-alive de conexiones
- Pool size es fijo (no dinámico)

Estas limitaciones son aceptables para un proyecto académico centrado en demostrar comprensión de sockets y HTTP básico.

## 📖 Referencias

- [RFC 2616 - HTTP/1.1](https://tools.ietf.org/html/rfc2616)
- [Java ServerSocket Documentation](https://docs.oracle.com/javase/8/docs/api/java/net/ServerSocket.html)
- [Java ExecutorService Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html)

## 👨‍💻 Autor

Proyecto Académico - 2025

---

**¡El servidor está listo para usarse y defenderse en revisión oral!**
