# 🌐 Aplicaciones para Comunicaciones en Red

## 📚 Descripción General

Colección de proyectos de **comunicaciones en red** que implementan conceptos fundamentales de programación de redes usando diferentes protocolos, arquitecturas y patrones de comunicación. Cada proyecto resuelve problemáticas reales de conectividad, transmisión de datos y arquitecturas distribuidas.

**Tecnologías principales:** Java, Node.js, Angular, Sockets TCP/UDP, HTTP, WebSocket

---

## 🎯 Objetivos del Repositorio

- Implementar aplicaciones de red desde cero usando **sockets**
- Comprender y aplicar protocolos de capa de aplicación (HTTP, WebSocket)
- Dominar patrones de comunicación **cliente-servidor**
- Implementar algoritmos de **control de flujo** y **descarga recursiva**
- Desarrollar **interfaces gráficas** para aplicaciones de red
- Aplicar conceptos de **concurrencia** y **manejo de hilos**

---

## 📂 Proyectos Implementados

### 🛒 [Proyecto 1 - Tienda en línea](Practica%201%20-%20Tienda%20en%20línea/)

**Concepto:** Aplicación de comercio electrónico con carrito de compras

**Tecnologías:**
- Java + JavaFX
- Sockets TCP bloqueantes
- Serialización JSON
- Interfaz web con WebView

**Funcionalidades:**
- Catálogo de productos con búsqueda y filtrado
- Carrito de compras con gestión de existencias
- Comunicación cliente-servidor con sockets TCP
- Interfaz gráfica responsive

**Aprendizajes:** Sockets TCP, protocolo cliente-servidor, manejo de JSON, validación de datos

---

### 🎵 [Proyecto 2 - Transmisión de MP3](Practica%202%20-%20Transmicion%20de%20MP3/)

**Concepto:** Streaming de audio con control de flujo

**Tecnologías:**
- Java + JavaFX
- Sockets UDP (DatagramSocket)
- Algoritmo Go-Back-N
- JavaFX Media Player

**Funcionalidades:**
- Transmisión de archivos MP3 por UDP
- Implementación del protocolo Go-Back-N para confiabilidad
- Reconstrucción de archivo en el cliente
- Reproductor de audio con controles (play/pause/stop)
- Barra de progreso

**Aprendizajes:** Sockets UDP, control de flujo, ventanas deslizantes, retransmisión, streaming

---

### 💬 [Proyecto 3 - Chat Grupal](Practica%203%20-%20Chat/)

**Concepto:** Sistema de chat en tiempo real con arquitectura híbrida

**Tecnologías:**
- Frontend: Angular + WebSocket
- Puente: Node.js + Express + dgram
- Backend: Java + UDP + Hilos
- Docker + Docker Compose

**Funcionalidades:**
- Chat grupal en tiempo real
- Salas de chat múltiples
- WebSocket para comunicación frontend-puente
- UDP para comunicación puente-backend
- Gestión de usuarios y mensajes
- Arquitectura de microservicios

**Aprendizajes:** WebSocket, UDP, arquitectura de puente, traducción de protocolos, concurrencia, Docker

---

### 🌐 [Proyecto 4 - HTTP Server](Practica%204%20-%20HTTP%20Server/)

**Concepto:** Servidor HTTP desde cero con concurrencia y balanceo

**Tecnologías:**
- Java SE puro (sin frameworks)
- ServerSocket + Socket
- ExecutorService (pool de hilos)
- HTTP/1.1 manual

**Funcionalidades:**
- Parsing manual de peticiones HTTP
- Métodos: GET, POST, PUT, DELETE, HEAD, TRACE
- Múltiples tipos MIME
- Pool de hilos para concurrencia
- Redirección automática con servidor secundario
- Interfaz web para pruebas

**Aprendizajes:** Protocolo HTTP, parsing de texto, sockets TCP, concurrencia, balanceo de carga

---

### 📥 [Proyecto 5 - HTTP Descargador de archivos](Practica%205%20-%20HTTP%20Descargador%20de%20archivos/)

**Concepto:** Servidor HTTP con capacidad de descarga recursiva (tipo wget)

**Tecnologías:**
- Java SE puro (sin frameworks)
- ServerSocket + Socket
- Cliente HTTP desde cero
- Parsing de HTML
- Hilos para descargas concurrentes

**Funcionalidades:**
- Servidor HTTP completo
- Cliente HTTP para descargas remotas
- Descarga recursiva (comportamiento tipo `wget -r`)
- Extracción de enlaces de HTML
- Servir archivos locales
- Múltiples tipos MIME
- Interfaz web moderna

**Aprendizajes:** Cliente/servidor HTTP, descarga recursiva, parsing HTML, algoritmos de rastreo, concurrencia

---

## 🛠️ Tecnologías y Herramientas

### Lenguajes
- **Java** (JDK 8, 11, 17)
- **JavaScript** (Node.js, ES6+)
- **TypeScript** (Angular)

### Frameworks y Librerías
- **JavaFX** - Interfaces gráficas
- **Angular 20** - Frontend web
- **Express.js** - Servidor web Node.js
- **Maven** - Gestión de dependencias Java

### Protocolos
- **TCP** - Conexiones confiables
- **UDP** - Datagramas sin conexión
- **HTTP/1.1** - Protocolo de transferencia de hipertexto
- **WebSocket** - Comunicación bidireccional en tiempo real

### Conceptos de Redes
- Sockets (ServerSocket, Socket, DatagramSocket)
- Control de flujo (Go-Back-N)
- Parsing de protocolos
- Cliente-servidor
- Arquitecturas híbridas
- Concurrencia y paralelismo
- Balanceo de carga

---

## 🚀 Requisitos Generales

### Software Necesario
- **Java JDK 8+** (recomendado JDK 11 o 17)
- **Apache Maven 3.6+**
- **Node.js 16+** (para Proyecto 3)
- **Docker** (opcional, para Proyecto 3)

### IDEs Recomendados
- **Visual Studio Code** con extensiones de Java
- **IntelliJ IDEA**
- **Eclipse**

---

## 📖 Cómo Usar Este Repositorio

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red.git
   cd Aplicaciones-para-comunicaciones-en-red
   ```

2. **Navegar al proyecto deseado:**
   ```bash
   cd "Practica X - Nombre"
   ```

3. **Leer el README específico de cada proyecto** para instrucciones detalladas

4. **Compilar y ejecutar según las instrucciones de cada proyecto**

---

## 📋 Estructura de Cada Proyecto

Todos los proyectos siguen una estructura similar:

```
Practica X - Nombre/
├── README.md           # Documentación completa
├── pom.xml            # Configuración Maven (Java)
├── package.json       # Configuración npm (Node.js)
├── src/               # Código fuente
│   ├── main/
│   │   ├── java/     # Clases Java
│   │   └── resources/ # Recursos (HTML, CSS, JS)
│   └── test/         # Pruebas unitarias
├── target/           # Archivos compilados
└── www/              # Archivos web estáticos
```

---

## 🎓 Conceptos Técnicos Implementados

### Programación de Sockets
- ✅ Sockets TCP (bloqueantes y no bloqueantes)
- ✅ Sockets UDP (DatagramSocket)
- ✅ ServerSocket y aceptación de conexiones
- ✅ Lectura y escritura de streams

### Protocolos de Red
- ✅ Diseño e implementación de protocolos personalizados
- ✅ Parsing manual de HTTP/1.1
- ✅ WebSocket para comunicación bidireccional
- ✅ Serialización de datos (JSON, texto, binario)

### Control de Flujo y Confiabilidad
- ✅ Algoritmo Go-Back-N
- ✅ Ventanas deslizantes
- ✅ Acknowledgments (ACK/NACK)
- ✅ Retransmisión de paquetes

### Arquitecturas de Red
- ✅ Cliente-Servidor tradicional
- ✅ Arquitectura de puente (bridge)
- ✅ Microservicios con Docker
- ✅ Balanceo de carga simple

### Concurrencia
- ✅ Pool de hilos (ExecutorService)
- ✅ Thread-safety con AtomicInteger
- ✅ Sincronización de recursos compartidos
- ✅ Manejo de múltiples conexiones simultáneas

---

## 👥 Desarrolladores

- **Velazquez Parral Saul Asaph**
- **Amador Martinez Jocelyn Lucia**

**Año:** 2025

---

## 📝 Notas Importantes

- El código prioriza **legibilidad** y **mantenibilidad** sobre micro-optimizaciones
- Cada proyecto está **bien documentado** con README completo
- Implementaciones desde cero sin dependencias externas para demostrar comprensión profunda de los protocolos
- Para uso en producción, se recomienda agregar capas adicionales de seguridad y validación

---

## 🔗 Enlaces Útiles

- **Repositorio:** [GitHub](https://github.com/Asaph-Velazquez/Aplicaciones-para-comunicaciones-en-red)
- **RFC HTTP/1.1:** [RFC 2616](https://tools.ietf.org/html/rfc2616)
- **Java Socket API:** [Documentación](https://docs.oracle.com/javase/8/docs/api/java/net/package-summary.html)
- **WebSocket Protocol:** [RFC 6455](https://tools.ietf.org/html/rfc6455)

---

**🎯 ¡Repositorio listo para aprender y practicar programación de redes!**
