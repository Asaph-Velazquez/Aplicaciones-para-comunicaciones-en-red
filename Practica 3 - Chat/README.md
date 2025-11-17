# Chat Grupal UDP-WebSocket Bridge

## Descripción General

Proyecto de **comunicación en red** con arquitectura integrada:

- **Frontend Angular**: Interfaz web que se conecta vía **WebSocket**
- **Puente Node.js**: Traduce WebSocket ↔ **UDP** (única vía de comunicación con backend)
- **Backend Java**: Servidor UDP con hilos que recibe/procesa mensajes

**Flujo único: Frontend (WS) → Puente (UDP) → Backend (UDP)**

---

## Arquitectura

```
┌────────────────────────────────────────────────────┐
│                                                    │
│  Navegador (Angular)                              │
│  └─ WebSocket:8080                               │
│         ↓                                          │
│  ┌──────────────────────────┐                    │
│  │   Puente Node.js         │                    │
│  │   • WebSocket :8080      │                    │
│  │   • UDP :5000            │                    │
│  │   • SalaManager (estado) │                    │
│  └──────────────────────────┘                    │
│         ↓                                          │
│  Backend Java (UDP:5000)                          │
│  └─ ChatServer (hilos)                            │
│     └─ ClientHandler por usuario                  │
│                                                    │
└────────────────────────────────────────────────────┘
```

### Componentes

| Componente | Tecnología | Puerto | Rol |
|-----------|-----------|--------|-----|
| **Frontend** | Angular 20 + WebSocket | WS:8080 | UI del chat |
| **Puente** | Node.js + Express + dgram | UDP:5000, WS:8080 | Traduce WS → UDP |
| **Backend** | Java 17 + DatagramSocket | UDP:5000 | Procesa datos |

---

## Flujo de Comunicación

### 1. **Conexión (JOIN)**

```
Frontend (Angular)          Puente Node.js          Backend Java
    │                           │                        │
    ├─ WS: {tipo:'JOIN'} ──────>│                       │
    │    usuario: 'Daniel'      │─ UDP: JOIN|Daniel     │
    │    sala: 'General'        │─────────────────────>│
    │                           │                   [respuesta]
    │                           │<───── UDP ────────────┤
    │<─ WS: UNIDO_SALA ─────────┤                       │
    │    usuarios: [...]        │                       │
```

**Proceso:**
1. Frontend envía JSON: `{ tipo: 'JOIN', usuario, sala }`
2. Puente traduce a UDP: `JOIN|Daniel|General`
3. Backend recibe en ClientHandler (nuevo hilo)
4. Backend registra usuario en SalaManager
5. Backend retorna lista de usuarios
6. Puente traduce respuesta a WebSocket JSON
7. Frontend recibe y actualiza UI

---

### 2. **Mensaje Público (SEND)**

```
Frontend (Angular)          Puente Node.js          Backend Java
    │                           │                        │
    ├─ WS: {tipo:'SEND'} ──────>│                       │
    │    usuario: 'Daniel'      │─ UDP: SEND|Daniel    │
    │    sala: 'General'        │         |General      │
    │    contenido: 'Hola!'     │         |Hola!        │
    │                           │─────────────────────>│
    │                           │                   [procesa]
    │                           │<── UDP broadcast ─────┤
    │<─ WS: NUEVO_MENSAJE ──────┤                       │
    │    usuario: 'Daniel'      │                       │
    │    contenido: 'Hola!'     │                       │
    │    privado: false         │                       │
```

**Proceso:**
1. Frontend envía: `{ tipo: 'SEND', usuario, sala, contenido }`
2. Puente traduce a UDP: `SEND|Daniel|General|Hola!`
3. Backend retransmite a todos en la sala
4. Puente recibe respuesta UDP
5. Puente envía JSON a frontend: `{ tipo: 'NUEVO_MENSAJE', usuario, contenido, privado: false }`
6. Frontend renderiza sin tags de protocolo

---

### 3. **Mensaje Privado (PRIVATE)**

```
Frontend (Angular)          Puente Node.js          Backend Java
    │                           │                        │
    ├─ WS: PRIVATE ────────────>│                       │
    │    usuario: 'Daniel'      │─ UDP: PRIVATE         │
    │    destinatario: 'Ana'    │        |Daniel|Ana    │
    │    contenido: 'Hola! 😀'  │        |Hola! 😀       │
    │                           │─────────────────────>│
    │                           │                   [procesa]
    │                           │
    │  ✅ EMISOR ve su mensaje  │
    │<─ WS: NUEVO_MENSAJE ──────┤
    │    usuario: 'Daniel'      │
    │    contenido: 'Hola! 😀'  │
    │    privado: true          │
    │    destinatario: 'Ana'    │
    │                           │
    │  ✅ DESTINATARIO ve mensaje
    │   (conectado por otro WS) │
```

**Proceso:**
1. Frontend envía: `{ tipo: 'PRIVATE', usuario, destinatario, contenido }`
2. Puente traduce a UDP: `PRIVATE|Daniel|Ana|Hola! 😀`
3. Backend procesa: envía solo a Ana
4. **Puente TAMBIÉN envía copia a Daniel** (echo)
5. Ambos reciben: `{ tipo: 'NUEVO_MENSAJE', privado: true, destinatario }`
6. Frontend renderiza con badge 🔒 si es privado
7. **Bonus**: Si solo emoji → renderiza con tamaño especial

---

## Protocolo UDP (Backend)

### Comandos recibidos por Backend

| Comando | Formato | Ejemplo |
|---------|---------|---------|
| JOIN | `JOIN\|usuario\|sala` | `JOIN\|Daniel\|General` |
| SEND | `SEND\|usuario\|sala\|mensaje` | `SEND\|Daniel\|General\|¡Hola!` |
| LEAVE | `LEAVE\|usuario\|sala` | `LEAVE\|Daniel\|General` |
| PRIVATE | `PRIVATE\|emisor\|destinatario\|msg` | `PRIVATE\|Daniel\|Ana\|Hola 😀` |

---

## Protocolo WebSocket (Frontend)

### Comandos enviados desde Frontend

```json
{
  "tipo": "JOIN",
  "usuario": "Daniel",
  "sala": "General",
  "contenido": "JOIN|Daniel|General",
  "timestamp": "2025-11-16T10:30:00Z"
}
```

### Notificaciones recibidas en Frontend

| Notificación | Campos | Cuándo |
|--------------|--------|--------|
| `CONEXION_EXITOSA` | `clientId` | Al conectar |
| `ACTUALIZAR_USUARIOS` | `usuarios`, `sala` | Cambio en sala |
| `UNIDO_SALA` | `usuarios`, `sala` | Tras JOIN |
| `NUEVO_MENSAJE` | `usuario`, `contenido`, `sala`, `privado`, `destinatario`, `timestamp` | Nuevo mensaje público o privado |

---

## Arranque Rápido

### Requisitos

**Opción A: Docker (Recomendado para Producción)**
- Docker Desktop instalado

**Opción B: Local (Desarrollo)**
- Java 17+
- Node.js 18+
- npm

---

### Iniciar con Docker Compose ⭐⭐⭐

```powershell
docker-compose up --build
```

Accede a:
- **Frontend**: http://localhost
- **WebSocket**: ws://localhost:8080
- **Backend UDP**: localhost:5000/udp

Ver detalles en [DOCKER.md](DOCKER.md)

---

### Iniciar Localmente (3 Terminales)

**Terminal 1: Backend Java**
```bash
cd "Chat Grupal"
./mvnw clean package -DskipTests
java -jar target/*.jar
```

**Terminal 2: Puente Node.js**
```bash
npm install
node server.js
```

**Terminal 3: Frontend Angular**
```bash
cd FrontEnd
npm install
npm start
```

Accede a: http://localhost:4200

---

## Verificación

### Estado del Puente

```bash
curl http://localhost:8080/status
```

Respuesta:
```json
{
  "estado": "activo",
  "clientesWebSocket": 1,
  "salas": [
    {
      "nombre": "General",
      "usuarios": ["Daniel", "Ana"],
      "totalMensajes": 5
    }
  ]
}
```

### Listar Salas Activas

```bash
curl http://localhost:8080/salas
```

---

## Estructura del Proyecto Actual

```
Practica 3 - Chat/
│
├── Chat Grupal/                    # Backend Java UDP
│   ├── src/main/java/ChatGrupal/demo/
│   │   ├── ChatServer.java         ⭐ Servidor UDP (DatagramSocket)
│   │   ├── ChatClient.java         ⭐ Cliente UDP (para pruebas)
│   │   └── config/
│   │       └── WebSocketConfig.java (NO se usa - solo UDP)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .dockerignore
│   └── README.md                   # 📖 UDP + Hilos detallado
│
├── FrontEnd/                       # Frontend Angular
│   ├── src/app/
│   │   ├── components/
│   │   │   ├── login/
│   │   │   └── chat-room/
│   │   ├── services/
│   │   │   └── chat.service.ts     # WebSocket ↔ Puente
│   │   └── models/
│   │       └── message.model.ts
│   ├── nginx.conf
│   ├── Dockerfile
│   ├── .dockerignore
│   └── README.md                   # 📖 Tratamiento de datos
│
├── server.js                       # 🌉 Puente Node.js (ÚNICO punto de traducción)
│   ├── SalaManager class           # Estado compartido
│   ├── UDP listener (:5000)
│   ├── WebSocket server (:8080)
│   └── procesarComandoUDP()        # Traduce UDP → JSON
│
├── docker-compose.yaml             # Orquestación Docker
├── Dockerfile                      # Puente
├── .dockerignore
├── .gitignore
├── package.json
├── start-services.ps1              # ⭐ Script único para desarrollo
│
├── README.md                       # 📍 Este archivo
├── Chat Grupal/README.md           # Backend UDP + Hilos
├── FrontEnd/README.md              # Frontend Angular
└── DOCKER.md                       # Docker deployment
```

---

## Características

✅ **UDP Único**: Única vía de comunicación Backend ↔ Puente  
✅ **WebSocket Limpio**: Frontend ↔ Puente en JSON limpio  
✅ **Hilos UDP**: `ClientHandler` por usuario en Backend  
✅ **Puente Stateful**: `SalaManager` mantiene estado real-time  
✅ **Mensajes Privados**: Emisor recibe echo (ve su mensaje)  
✅ **Emojis**: Detección automática, renderizado especial  
✅ **Sin Comandos en UI**: Frontend ve solo contenido limpio  
✅ **Docker Ready**: 3 contenedores orquestados  
✅ **Documentación Actual**: 3 READMEs especializados  

---

## Troubleshooting

| Error | Causa | Solución |
|-------|-------|----------|
| `EADDRINUSE :::8080` | Puerto ocupado | `netstat -ano \| findstr :8080` y terminar proceso |
| `java.net.BindException: Address already in use :5000` | UDP:5000 ocupado | Terminar proceso Java o Docker anterior |
| WebSocket no conecta | Puente no corre | Ejecutar `node server.js` |
| Mensajes no llegan | UDP bloqueado | Verificar firewall |
| Docker: "Cannot connect to Docker daemon" | Docker Desktop apagado | Abrir Docker Desktop |

---

## Documentación Especializada

- **Backend UDP + Hilos**: [`Chat Grupal/README.md`](Chat\ Grupal/README.md) - Sincronización, threads, protocol
- **Frontend Tratamiento de Datos**: [`FrontEnd/README.md`](FrontEnd/README.md) - WebSocket, parsing, signals
- **Docker Deployment**: [`DOCKER.md`](DOCKER.md) - Containerización, compose, troubleshooting

---

**Proyecto**: Redes 2 - Práctica 3  - Chat de grupo
**Autor**: Velazquez Parral Saul Asaph
**Fecha**: Noviembre 2025  