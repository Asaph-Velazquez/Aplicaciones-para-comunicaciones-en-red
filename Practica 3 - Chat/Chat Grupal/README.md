# Backend: Servidor UDP con Hilos

## Descripción General

Este backend implementa un **servidor de chat con sockets de datagrama (UDP) e hilos** que:
- Recibe conexiones UDP de múltiples clientes en paralelo
- Mantiene lista de usuarios por sala
- Retransmite actualizaciones de usuarios y mensajes
- Usa un hilo separado por cliente para evitar bloqueos

---

## Arquitectura UDP + Hilos

### DatagramSocket (UDP)

**¿Por qué UDP?**
- Sin conexión: no requiere handshake (vs TCP)
- Broadcast-friendly: ideal para chats grupal
- Bajo overhead: pequeños paquetes de control

**Socket UDP en Java:**
```java
DatagramSocket serverSocket = new DatagramSocket(5000);
byte[] receiveBuffer = new byte[1024];

// Recibir paquete
DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
serverSocket.receive(receivePacket);

// Enviar respuesta
DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, receivePacket.getAddress(), receivePacket.getPort());
serverSocket.send(sendPacket);
```

---

### Hilos para Manejo Concurrente

**Problema sin hilos:** Si un cliente se retrasa, bloquea a los demás.  
**Solución:** Un hilo por cliente.

```java
// Servidor
while (true) {
    DatagramPacket receivePacket = ...;
    serverSocket.receive(receivePacket);
    
    // ✅ Crear nuevo hilo para este cliente
    new Thread(new ClientHandler(receivePacket, serverSocket, salas)).start();
}

// Hilo del cliente
class ClientHandler implements Runnable {
    @Override
    public void run() {
        // Procesar comando del cliente sin bloquear servidor
        String mensaje = new String(packet.getData(), 0, packet.getLength());
        String[] partes = mensaje.split("\\|");
        
        if ("JOIN".equals(partes[0])) {
            String usuario = partes[1];
            String sala = partes[2];
            agregarUsuarioASala(usuario, sala);
        }
    }
}
```

**Ventajas:**
- Cada cliente procesa independientemente
- Servidor nunca se bloquea esperando un cliente lento
- Manejo de miles de conexiones simultáneas

---

## Protocolo de Comandos UDP

### Formato: `COMANDO|param1|param2|...`

| Comando | Params | Efecto |
|---------|--------|--------|
| `JOIN` | `usuario`, `sala` | Agrega usuario a sala; retransmite lista actualizada |
| `LEAVE` | `usuario`, `sala` | Remueve usuario; retransmite lista |
| `SEND` | `usuario`, `sala`, `mensaje` | Mensaje grupal en sala |
| `PRIVATE` | `usuario`, `destinatario`, `mensaje` | Mensaje privado |
| `LIST` | `sala` | Responde con lista de usuarios |

**Ejemplos:**
```
JOIN|Daniel|General        → Usuario Daniel entra a sala General
SEND|Daniel|General|Hola!  → Daniel envía "Hola!" a General
PRIVATE|Daniel|Ana|¿Cómo?  → Mensaje privado Daniel → Ana
LEAVE|Daniel|General       → Daniel se va de General
```

---

## Gestión de Salas

### Estructura de Datos

```java
Map<String, Set<String>> salas = new HashMap<>();
// Ejemplo:
// "General" → {"Daniel", "Ana", "Carlos"}
// "Trabajo" → {"Ana"}
```

### Sincronización con Locks

```java
private static final Object lock = new Object();

synchronized (lock) {
    salas.computeIfAbsent(sala, k -> new HashSet<>()).add(usuario);
}
```

**¿Por qué?** Múltiples hilos acceden el Map simultáneamente → necesita sincronización.

---

## Clases Principales

### 1. **ChatServer**
- Punto de entrada
- Crea `DatagramSocket` en puerto 5000
- Bucle infinito: recibe → crea hilo

### 2. **ClientHandler**
- Implementa `Runnable`
- Se ejecuta en hilo propio
- Procesa comandos del cliente
- Actualiza estado del servidor

### 3. **Métodos Clave**

```java
agregarUsuarioASala(usuario, sala)
  ├─ Sincroniza acceso al Map
  ├─ Agrega usuario al HashSet de la sala
  └─ Retorna lista actualizada

removerUsuarioDeSala(usuario, sala)
  ├─ Sincroniza acceso al Map
  └─ Remueve usuario

responderAlCliente(usuario, sala)
  └─ Envía lista de usuarios via UDP al cliente

enviarRespuesta(respuesta)
  └─ Crea DatagramPacket y lo envía al socket del cliente
```

---

## Flujo de Ejecución

### Cliente JOIN

```
1. Cliente UDP envía: "JOIN|Daniel|General"
   ↓
2. ServerSocket.receive() recibe paquete
   ↓
3. Servidor crea: new Thread(new ClientHandler(...)).start()
   ↓
4. ClientHandler.run() ejecuta:
   - Parsea comando: ["JOIN", "Daniel", "General"]
   - Sincroniza: salas.get("General").add("Daniel")
   - Llama: responderAlCliente("Daniel", "General")
   ↓
5. responderAlCliente() envía UDP:
   "OK|Daniel,Ana,Carlos"
   ↓
6. Cliente recibe lista actualizada
```

---

## Manejo de Concurrencia

### Problema: Race Conditions

```
Thread 1: if (!salas.has("General"))  ← lee
Thread 2: salas.set("General", ...)    ← escribe (en medio)
Thread 1: salas.get("General").add()   ← ¡ERROR! NPE
```

### Solución: `synchronized`

```java
synchronized (lock) {
    // Solo 1 hilo ejecuta aquí a la vez
    salas.computeIfAbsent(sala, k -> new HashSet<>()).add(usuario);
}
```

---

## Compilación y Ejecución

### Compilar

```bash
cd "Chat Grupal/src/main/java"
javac ChatGrupal/demo/ChatServer.java
javac ChatGrupal/demo/ChatClient.java
```

### Ejecutar Servidor

```bash
java ChatGrupal.demo.ChatServer
# Salida:
# Iniciando Servidor de Chat...
# Escuchando en puerto: 5000
```

### Ejecutar Clientes (en otras terminales)

```bash
java ChatGrupal.demo.ChatClient
# Ingresa comando: JOIN Daniel General
# Respuesta: OK|Daniel
```

---

## Limitaciones y Mejoras

| Aspecto | Actual | Mejora |
|--------|--------|--------|
| Persistencia | No (en memoria) | Base de datos |
| Seguridad | Sin encriptación | TLS/SSL |
| Validación | Mínima | Regex completo |
| Logging | println | Log4j/SLF4J |
| Testing | Manual | JUnit + Mockito |

---

## Integración con Puente Node.js

Este servidor UDP se integra con `server.js` (puente Node.js):

```
Clientes Java ----UDP----> server.js (relay) ---WebSocket---> Angular
```

El puente `server.js`:
1. Escucha UDP en puerto 5000
2. Mantiene estado de salas (SalaManager)
3. Procesa comandos de clientes UDP y WebSocket
4. Traduce mensajes: UDP → JSON → WebSocket

Ver `README.md` en raíz para flujo completo.

---

**Resumen:** Backend UDP robusto con hilos para manejo concurrente, lista de usuarios sincronizada y protocolo simple basado en pipes (|).
                  |
     ┌────────────┼────────────┐
     |            |            |
  Hilo 1       Hilo 2       Hilo N
  Cliente A   Cliente B    Cliente C
     \           |           /
      \          |          /
       \         |         /
        └─ Sala "General" ─┘
        
    Usuarios: [Alice, Bob, Charlie]
```

### ChatClient.java - Cliente Principal

**Propósito:** Permite a usuarios conectarse a salas y enviar/recibir mensajes.

```java
// Cliente UDP
DatagramSocket clientSocket = new DatagramSocket();

// Hilo receptor en paralelo para escuchar respuestas
new Thread(new ReceiverThread()).start();

// Hilo principal: Interfaz de usuario
while (activo) {
    // Envía comandos (JOIN, SEND, LEAVE)
}
```

**Arquitectura Cliente:**
```
┌──────────────────────────────┐
│      Hilo Principal          │
│  - Interfaz de usuario       │
│  - Lee comandos              │
│  - Envía packets UDP         │
└──────────────────────────────┘
            │
     ┌──────┴──────┐
     │             │
┌────▼─────────────▼────┐
│   Hilo Receptor       │
│ - Escucha servidor    │
│ - Recibe mensajes     │
│ - Actualiza pantalla  │
└───────────────────────┘
```

---

## 🧵 Implementación de Hilos

### Servidor: ClientHandler (Procesamiento de cliente)

```java
static class ClientHandler implements Runnable {
    private DatagramPacket packet;           // Datos del cliente
    private DatagramSocket socket;           // Socket del servidor
    private Map<String, Set<String>> salas;  // Salas compartidas
    
    @Override
    public void run() {
        String mensaje = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Hilo " + Thread.currentThread().getId() + 
                         " procesando: " + mensaje);
        
        // Procesa comando (JOIN, SEND, LEAVE)
        procesarComando(mensaje);
        
        // Retransmite lista de usuarios actualizada
        retransmitirListaUsuarios();
    }
}
```

**Características:**
- ✅ Un hilo por cliente conectado
- ✅ Acceso sincronizado a `salas` con `synchronized(lock)`
- ✅ No bloquea otros clientes
- ✅ Escalable a muchos usuarios

### Cliente: ReceiverThread (Recepción de mensajes)

```java
class ReceiverThread implements Runnable {
    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            // Escucha indefinidamente
            clientSocket.receive(packet);
            
            String mensaje = new String(packet.getData(), 0, packet.getLength());
            System.out.println("\n📩 " + mensaje);
            System.out.print("> ");  // Retorna el prompt
        }
    }
}
```

**Características:**
- ✅ Corre en paralelo al hilo principal
- ✅ Escucha sin bloquear entrada del usuario
- ✅ Actualiza pantalla automáticamente

---

## 📡 Protocolo de Comunicación UDP

### Formato de Mensaje

```
COMANDO Sala:<nombre_sala> [parametros]
```

### 1. JOIN - Unirse a Sala

**Cliente envía:**
```
JOIN Sala:General
```

**Servidor responde:**
```
SALA_USUARIOS General: [Alice, Bob, Charlie]
```

**Flujo en el servidor:**
```java
if (comando.equals("JOIN")) {
    String sala = extraerSala(mensaje);
    
    // Sincronización segura
    synchronized (lock) {
        Set<String> usuarios = salas.getOrDefault(sala, new HashSet<>());
        usuarios.add(usuario);
        salas.put(sala, usuarios);
    }
    
    // Retransmite a todos en la sala
    retransmitirLista(sala, usuarios);
}
```

### 2. SEND - Enviar Mensaje

**Cliente envía:**
```
SEND Sala:General ¡Hola a todos!
```

**Servidor retransmite a todos en sala:**
```
Alice: ¡Hola a todos!
```

### 3. LEAVE - Abandonar Sala

**Cliente envía:**
```
LEAVE Sala:General
```

**Servidor:**
- Elimina usuario de sala
- Retransmite lista actualizada
- Si sala está vacía, la elimina

### 4. PRIVATE - Mensaje Privado

**Cliente envía:**
```
PRIVATE Bob Tu mensaje secreto
```

**Servidor envía directamente a Bob:**
```
Privado de Alice: Tu mensaje secreto
```

---

## 🔐 Thread Safety & Sincronización

### El Problema

```java
// ❌ INSEGURO - Condición de carrera
Set<String> usuarios = salas.get("General");
usuarios.add("Alice");  // Alice se puede agregar dos veces
salas.put("General", usuarios);
```

Con múltiples hilos:
```
Hilo 1: Lee usuarios = [Bob]
Hilo 2: Lee usuarios = [Bob]        ← Problema: lee sin cambios
Hilo 1: Agrega Alice → [Bob, Alice]
Hilo 2: Agrega Charlie → [Bob, Charlie]  ← Se perdió Alice!
```

### La Solución

```java
// ✅ SEGURO - Usa sincronización
private static final Object lock = new Object();

synchronized (lock) {
    Set<String> usuarios = salas.getOrDefault(sala, new HashSet<>());
    usuarios.add(usuario);
    salas.put(sala, usuarios);
}
```

**Resultado con sincronización:**
```
Hilo 1: [BLOQUEA lock]
        Lee usuarios = [Bob]
        Agrega Alice → [Bob, Alice]
        Libera lock
Hilo 2: [ESPERA lock]
        [ADQUIERE lock]
        Lee usuarios = [Bob, Alice]  ← Correcto!
        Agrega Charlie → [Bob, Alice, Charlie]
        Libera lock
```

---

## 🚀 Ejecución: UDP + Hilos

### Terminal 1 - Compilar y Ejecutar Servidor

```powershell
cd "Chat Grupal"
cd src/main/java

# Compilar
javac ChatGrupal/demo/ChatServer.java

# Ejecutar
java ChatGrupal.demo.ChatServer
```

**Salida esperada:**
```
Iniciando Servidor de Chat...
Escuchando en puerto: 5000
Hilo 140 procesando: JOIN Sala:General
Hilo 141 procesando: SEND Sala:General ¡Hola!
```

### Terminal 2 - Compilar y Ejecutar Cliente 1

```powershell
cd "Chat Grupal"
cd src/main/java

# Compilar
javac ChatGrupal/demo/ChatClient.java

# Ejecutar
java ChatGrupal.demo.ChatClient
```

**Interacción del Usuario:**
```
=== Cliente de Chat Grupal ===
Ingrese su nombre de usuario: Alice

=== Menu ===
1. Unirse a una sala
2. Listar usuarios en sala actual
3. Enviar mensaje
4. Enviar mensaje privado
5. Abandonar sala
6. Salir

Seleccione una opción: 1
Ingrese nombre de sala: General
✓ Unido a sala: General

📩 SALA_USUARIOS General: []

Seleccione una opción: 3
Ingrese mensaje: ¡Hola a todos!
✓ Mensaje enviado

📩 Alice: ¡Hola a todos!

Seleccione una opción: 5
✓ Sala abandonada
```

### Terminal 3 - Ejecutar Cliente 2 (Otro usuario)

```powershell
java ChatGrupal.demo.ChatClient
```

```
Ingrese su nombre de usuario: Bob

Seleccione una opción: 1
Ingrese nombre de sala: General
✓ Unido a sala: General

📩 SALA_USUARIOS General: [Alice]

Seleccione una opción: 3
Ingrese mensaje: ¡Hola Alice!
✓ Mensaje enviado

📩 Bob: ¡Hola Alice!
📩 Alice: ¡Hola a todos!
```

**Resultado:**
- ✅ Alice ve los mensajes de Bob en tiempo real
- ✅ Bob ve los mensajes de Alice en tiempo real
- ✅ Cada usuario en su propio hilo
- ✅ Comunicación UDP de baja latencia

---

## 📊 Ciclo de Vida Completo

```
┌─────────────┐
│  Servidor   │
│  escucha    │
│  en 5000    │
└──────┬──────┘
       │
┌──────▼──────────────┐
│  Cliente A conecta  │
│  (ReceiverThread)   │
└──────┬──────────────┘
       │
┌──────▼──────────────────────┐
│ Servidor crea ClientHandler │
│ en nuevo HILO para A        │
└──────┬──────────────────────┘
       │
┌──────▼───────────┐
│  A envía: JOIN   │
│  Sala:General    │
└──────┬───────────┘
       │
┌──────▼────────────────────────┐
│ ClientHandler de A:           │
│ - Sincroniza acceso a salas   │
│ - Agrega A a "General"        │
│ - Retransmite lista           │
└──────┬────────────────────────┘
       │
┌──────▼──────────────────────┐
│  Cliente B conecta          │
│  (ReceiverThread)           │
└──────┬──────────────────────┘
       │
┌──────▼──────────────────────────┐
│ Servidor crea ClientHandler     │
│ en NUEVO HILO para B            │
└──────┬──────────────────────────┘
       │
┌──────▼───────────┐
│  B envía: JOIN   │
│  Sala:General    │
└──────┬───────────┘
       │
┌──────▼────────────────────────┐
│ ClientHandler de B:           │
│ - Sincroniza acceso a salas   │
│ - Agrega B a "General"        │
│ - Retransmite lista           │
│   [A, B]                      │
└──────┬────────────────────────┘
       │
┌──────▼────────────────────────────┐
│ A recibe en ReceiverThread:       │
│ "SALA_USUARIOS General: [A, B]"   │
└──────────────────────────────────┘
```

---

## 📡 Comparación: UDP vs WebSocket

| Característica | UDP + Hilos | WebSocket |
|---|---|---|
| **Puerto** | 5000 | 8080 |
| **Protocolo** | DatagramSocket | HTTP → WebSocket |
| **Hilos** | ✅ Uno por cliente | ✅ Pool en Spring |
| **Conexión** | Sin estado | Persistente |
| **Latencia** | Muy baja | Baja |
| **Confiabilidad** | No garantizada | Garantizada |
| **Interfaz** | Consola | Navegador |
| **Escalabilidad** | Moderada (hilos) | Alta (Spring) |

---

## 🔧 Alternativa: WebSocket (Secundaria)

Si prefieres interfaz web en lugar de consola:

### Iniciar Backend WebSocket
```powershell
mvn spring-boot:run
```

### Iniciar Frontend
```powershell
cd FrontEnd
npm start
```

### Conceptos en WebSocket

**ChatController.java:**
```java
@MessageMapping("/chat/{sala}/join")
@SendTo("/topic/sala/{sala}")
public ChatMessage join(@DestinationVariable String sala, ChatMessage msg) {
    salaService.agregarUsuarioASala(sala, msg.getUsuario());
    return msg;
}
```

**SalaService.java:**
```java
public void agregarUsuarioASala(String sala, String usuario) {
    // ConcurrentHashMap proporciona thread-safety automática
    salas.computeIfAbsent(sala, k -> ConcurrentHashMap.newKeySet())
         .add(usuario);
}
```

---

## 📝 Logging Detallado

Ambos enfoques incluyen logging:

**Console:**
```
[2024-11-16 10:45:23] [pool-1-thread-1] INFO - Usuario Alice unido a sala: General
[2024-11-16 10:45:25] [pool-1-thread-2] INFO - Usuario Bob unido a sala: General
[2024-11-16 10:45:30] [pool-1-thread-1] WARN - Usuario Alice abandonó sala: General
```

**Archivos:**
- `logs/chat-aplicacion.log` - Eventos generales
- `logs/chat-operaciones.log` - Operaciones detalladas

---

## 🎓 Conceptos Clave Aprendidos

### ✅ Sockets de Datagrama (UDP)
```java
DatagramSocket serverSocket = new DatagramSocket(5000);  // Servidor
DatagramSocket clientSocket = new DatagramSocket();      // Cliente

// Envío
DatagramPacket packet = new DatagramPacket(datos, datos.length, 
                                          InetAddress.getByName("localhost"), 5000);
clientSocket.send(packet);

// Recepción
serverSocket.receive(packet);
```

### ✅ Hilos (Threading)
```java
// Crear hilo
new Thread(new ClientHandler(...)).start();

// Sincronización
synchronized (lock) {
    // Código crítico
}

// Espera activa
while (true) {
    socket.receive(packet);  // Bloquea hasta recibir
}
```

### ✅ Retransmisión a Múltiples Usuarios
```java
// Envía a cada usuario en la sala
for (String usuario : salas.get(sala)) {
    enviarA(usuario, mensajeRetransmitido);
}
```

---

## 📚 Archivos Principales

| Archivo | Propósito |
|---------|-----------|
| `ChatServer.java` | Servidor UDP principal |
| `ChatClient.java` | Cliente UDP principal |
| `ChatController.java` | Alternativa WebSocket |
| `SalaService.java` | Gestión de salas (WebSocket) |
| `ChatMessage.java` | DTO de mensajes |
| `Sala.java` | Modelo de sala |

---

## 🔗 Próximos Pasos

1. ✅ Ejecutar servidor UDP
2. ✅ Conectar múltiples clientes
3. ✅ Probar mensajes privados
4. ⚡ (Opcional) Usar interfaz web con WebSocket

---

**Para documentación general del proyecto:** Ver `README.md` en raíz
