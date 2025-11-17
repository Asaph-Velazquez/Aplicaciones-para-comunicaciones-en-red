# Frontend: Tratamiento de Datos desde WebSocket

## Descripción General

Frontend Angular que:
1. Se conecta al servidor WebSocket (Node.js bridge) en `ws://localhost:8080`
2. Recibe mensajes JSON estructurados
3. Procesa comandos y notificaciones
4. Actualiza la UI de forma reactiva con Signals

---

## Flujo de Datos: WebSocket → UI

### 1. Conexión Inicial

```typescript
// chat.service.ts
constructor() {}

conectar(nombreUsuario: string): Promise<boolean> {
  return new Promise((resolve, reject) => {
    const usuario: Usuario = { nombre: nombreUsuario, salas: [], activo: true };
    this.usuarioActual.set(usuario);
    
    // Crear conexión WebSocket
    this.ws = new WebSocket('ws://localhost:8080');
    
    this.ws.onopen = () => {
      this.conectado = true;
      resolve(true);  // ✅ Listo para recibir mensajes
    };
    
    this.ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.procesarMensaje(data);  // ← Punto clave
    };
  });
}
```

**Flujo:**
```
1. Usuario ingresa nombre
2. Componente Login → chatService.conectar()
3. WebSocket se abre
4. Frontend listo para recibir del servidor
```

---

### 2. Recepción y Procesamiento de Mensajes

#### **Entrada: JSON desde WebSocket**

```json
{
  "tipo": "NUEVO_MENSAJE",
  "usuario": "Daniel",
  "sala": "General",
  "contenido": "Hola a todos!",
  "timestamp": "2025-11-16T10:30:00.000Z",
  "privado": false
}
```

#### **Procesamiento: `procesarMensaje(data)`**

```typescript
private procesarMensaje(data: any) {
  console.log('[CHAT SERVICE] Mensaje recibido:', data);

  // PASO 1: Validar tipo de notificación
  if (data.tipo === 'CONEXION_EXITOSA') {
    console.log('✅ Conectado al servidor');
    return;
  }

  if (data.tipo === 'ACTUALIZAR_USUARIOS') {
    console.log('👥 Usuarios en sala:', data.usuarios);
    this.usuariosEnSala.set(data.usuarios || []);  // ← Signal reactiva
    return;
  }

  if (data.tipo === 'UNIDO_SALA') {
    console.log('✅ Unido a sala:', data.sala);
    this.usuariosEnSala.set(data.usuarios || []);
    return;
  }

  // PASO 2: Procesar nuevo mensaje
  if (data.tipo === 'NUEVO_MENSAJE') {
    // PASO 2A: Limpiar contenido (remover tags de protocolo)
    let contenido = data.contenido || '';
    if (typeof contenido === 'string') {
      const partes = contenido.split('|');
      // Si aún contiene "SEND|user|sala|msg" extraer solo el mensaje
      if (partes.length >= 4 && (partes[0] === 'SEND' || partes[0] === 'PRIVATE')) {
        contenido = partes.slice(3).join('|');
      }
    }

    // PASO 2B: Detectar si es emoji-only (para styling)
    let esSoloEmoji = false;
    try {
      esSoloEmoji = /^\p{Extended_Pictographic}+(\uFE0F|\u200D\p{Extended_Pictographic})*$/u.test(contenido.trim());
    } catch (e) {
      esSoloEmoji = contenido.trim().length <= 4 && /[^\w\s]/.test(contenido);
    }

    const tipoMensaje = esSoloEmoji ? 'emoji' : 'texto';
    const isPrivado = data.privado === true;

    // PASO 2C: Construir objeto Message
    const mensaje: Message = {
      id: Math.random().toString(),
      usuario: data.usuario,
      sala: data.sala || null,
      contenido: contenido,  // ← Contenido limpio (sin tags)
      tipo: isPrivado ? 'privado' : tipoMensaje,  // ← Tipo correcto para UI
      timestamp: data.timestamp ? new Date(data.timestamp) : new Date(),
      destinatario: data.destinatario
    };

    // PASO 2D: Filtrar si debe mostrarse
    const usuarioActual = this.usuarioActual()?.nombre;
    const esSalaActual = mensaje.sala && mensaje.sala === this.salaActual()?.nombre;
    const esPrivadoParaMi = isPrivado && (mensaje.destinatario === usuarioActual || mensaje.usuario === usuarioActual);

    // PASO 2E: Agregar a lista si cumple filtro
    if (esSalaActual || esPrivadoParaMi) {
      const msgs = this.mensajes();
      this.mensajes.set([...msgs, mensaje]);  // ← Signal reactiva
      console.log(`💬 Mensaje de ${mensaje.usuario}: ${mensaje.contenido}`);
    }
  }
}
```

---

## Transformación de Datos

### Entrada → Procesamiento → Salida

```
┌─────────────────────────────────────────────────────────┐
│ 1. RECIBIR: JSON desde WebSocket                        │
│ {tipo:'NUEVO_MENSAJE', usuario:'Daniel', sala:...}     │
└─────────────────────────┬───────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 2. LIMPIAR: Remover tags de protocolo                   │
│ "SEND|Daniel|General|Hola!" → "Hola!"                  │
└─────────────────────────┬───────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 3. DETECTAR: Tipo (emoji, privado, etc)                │
│ "😀" → tipo: 'emoji'                                    │
│ privado: true → tipo: 'privado'                        │
└─────────────────────────┬───────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 4. FILTRAR: ¿Mostrar en UI?                            │
│ ✅ Mensaje público de mi sala                          │
│ ✅ Mensaje privado para mí (emisor o destinatario)     │
│ ❌ Mensaje de otra sala                                │
└─────────────────────────┬───────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 5. ACTUALIZAR: Signal reactiva (Angular)               │
│ this.mensajes.set([...msgs, mensaje])                 │
│ ↓ UI se actualiza automáticamente                      │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ 6. RENDERIZAR: HTML con data binding                   │
│ <div>{{ mensaje.usuario }}: {{ mensaje.contenido }}</div>
└─────────────────────────────────────────────────────────┘
```

---

## Signals Reactivas (Estado)

```typescript
// Almacenan estado y notifican a componentes cuando cambian

public usuarioActual = signal<Usuario | null>(null);
// Cuando: conectar() completado
// Valor: { nombre: 'Daniel', salas: [], activo: true }

public salaActual = signal<Sala | null>(null);
// Cuando: unirseASala() enviado
// Valor: { nombre: 'General', usuarios: [], mensajes: [] }

public mensajes = signal<Message[]>([]);
// Cuando: recibir NUEVO_MENSAJE
// Valor: [{ id, usuario, sala, contenido, tipo, timestamp }, ...]

public usuariosEnSala = signal<string[]>([]);
// Cuando: recibir ACTUALIZAR_USUARIOS
// Valor: ['Daniel', 'Ana', 'Carlos']
```

---

## Componentes Principales

### chat.service.ts

**Responsabilidades:**
1. Abrir/cerrar WebSocket
2. Parsear JSON recibido
3. Limpiar contenido (remover protocolo)
4. Detectar emoji
5. Filtrar mensajes (privado/público)
6. Mantener estado (Signals)

**Métodos Clave:**
- `conectar(usuario)` → Abre WebSocket
- `procesarMensaje(data)` → **Procesa cada mensaje**
- `enviarMensaje(contenido)` → Envía mensaje grupal
- `enviarMensajePrivado(contenido, dest)` → Envía privado
- `unirseASala(sala)` → Envía JOIN

### chat-room.component.ts

**Responsabilidades:**
1. Acceder a `chatService` (inyectado)
2. Mostrar lista de mensajes filtrando privados
3. Mostrar lista de usuarios
4. Botones: enviar, emoji, abandonar, privado

**Template Binding:**
```html
@for (mensaje of obtenerMensajesFiltrados(); track mensaje.id) {
  <div class="mensaje" [class.mensaje-privado]="esMensajePrivado(mensaje)">
    <span>{{ mensaje.usuario }}</span>
    <div [class.mensaje-emoji]="mensaje.tipo === 'emoji'">
      {{ mensaje.contenido }}
    </div>
  </div>
}
```

---

## Tratamiento Especial: Mensajes Privados

### JSON Recibido

```json
{
  "tipo": "NUEVO_MENSAJE",
  "usuario": "Daniel",
  "contenido": "¿Cómo estás?",
  "privado": true,
  "destinatario": "Ana",
  "timestamp": "2025-11-16T10:35:00.000Z"
}
```

### Procesamiento

```typescript
const isPrivado = data.privado === true;
const tipoMensaje = isPrivado ? 'privado' : 'texto';

const mensaje: Message = {
  usuario: 'Daniel',
  contenido: '¿Cómo estás?',
  tipo: 'privado',  // ← Marca como privado
  destinatario: 'Ana'
};

// Mostrar solo si:
// - Yo soy Ana (destinatario)
// - O yo soy Daniel (emisor)
const esPrivadoParaMi = isPrivado && (
  mensaje.destinatario === usuarioActual ||
  mensaje.usuario === usuarioActual
);

if (esPrivadoParaMi) {
  this.mensajes.set([...msgs, mensaje]);  // ← Se agrega a lista
}
```

### Renderizado en HTML

```html
@if (esMensajePrivado(mensaje)) {
  <span class="badge-privado">🔒 Privado</span>
}
```

---

## Tratamiento Especial: Emojis

### Detección Unicode

```typescript
const contenido = "😀";  // Emoji

// Regex con bandera 'u' para Unicode
const esSoloEmoji = /^\p{Extended_Pictographic}+(\uFE0F|\u200D\p{Extended_Pictographic})*$/u
  .test(contenido.trim());
// → true

const tipoMensaje = esSoloEmoji ? 'emoji' : 'texto';
```

### Renderizado con CSS Especial

```html
<div [class.mensaje-emoji]="mensaje.tipo === 'emoji'">
  {{ mensaje.contenido }}
</div>

<!-- CSS -->
<style>
  .mensaje-emoji {
    font-size: 48px;  /* Emojis más grandes */
    text-align: center;
  }
</style>
```

---

## Resumen del Flujo

1. **WebSocket abierto** → Usuario listo para recibir
2. **Mensaje JSON llega** → onmessage event
3. **`procesarMensaje()`** → Limpia, detecta tipo, filtra
4. **Signal actualizada** → `this.mensajes.set(...)`
5. **Template reactivo** → `@for (msg of service.mensajes())`
6. **HTML renderizado** → Usuario ve mensaje en pantalla

**Ejemplo real:**
```
Server envía: {"tipo":"NUEVO_MENSAJE","usuario":"Daniel","contenido":"SEND|Daniel|General|¡Hola!","privado":false}
   ↓
Frontend recibe en onmessage()
   ↓
procesarMensaje() limpia: "¡Hola!"
   ↓
Crea Message object con tipo:'texto'
   ↓
Agrega a this.mensajes (Signal)
   ↓
chat-room.component.html ve cambio
   ↓
Re-renderiza @for loop
   ↓
Usuario ve: "Daniel: ¡Hola!" en pantalla
```

---

**Resumen:** El frontend recibe JSON limpio, lo procesa (extrae contenido real, detecta tipo/privado), lo filtra (solo si es para mí) y lo renderiza reactivamente.│  └──────────────────────────────────┘           │
│                 │                               │
│                 ↓                               │
│         Backend (Spring Boot)                  │
│                                                │
└──────────────────────────────────────────────────┘
```

## 📂 Estructura de Archivos

```
FrontEnd/src/app/
├── app.ts                          # Componente raiz
├── app.routes.ts                   # Definicion de rutas
├── app.config.ts                   # Configuracion Angular
│
├── components/
│   ├── login/
│   │   ├── login.component.ts      # Logica del login
│   │   ├── login.component.html    # Template
│   │   └── login.component.css     # Estilos
│   │
│   └── chat-room/
│       ├── chat-room.component.ts      # Logica del chat
│       ├── chat-room.component.html    # Template
│       └── chat-room.component.css     # Estilos
│
├── services/
│   └── chat.service.ts             # Servicio de comunicacion
│
└── models/
    └── message.model.ts            # Interfaces TypeScript
```

## 🔄 Flujo de Datos del Frontend

### 1. Inicializacion de la Aplicacion

```
Aplicacion Inicia
    │
    ↓
Angular carga app.ts
    │
    ├─ Define rutas
    │  ├─ '/' → Login Component
    │  └─ '/chat' → Chat Room Component
    │
    └─ Inicializa ChatService
       └─ Prepara conexion WebSocket
```

### 2. Pantalla de Login

```
Usuario accede a http://localhost:4200
    │
    ↓
Se muestra LoginComponent
    │
    ├─ Input: Nombre de usuario
    ├─ Input: Nombre de sala
    └─ Boton: Entrar al Chat
    │
    ↓
Usuario llena el formulario y presiona "Entrar"
    │
    ↓
LoginComponent valida datos
    │
    ├─ Usuario no vacio?
    ├─ Sala no vacia?
    └─ Todos los datos validos?
    │
    ↓
Si es valido:
    - Guarda usuario y sala en servicio
    - Navega a /chat
    - ChatRoomComponent se inicializa
    │
    └─ ChatService inicia conexion WebSocket
       ├─ Conecta a ws://localhost:8080/ws-chat
       ├─ Espera confirmacion de conexion
       └─ Una vez conectado, envia JOIN
```

### 3. Conexion WebSocket

```
ChatService.connect()
    │
    ↓
Crea cliente STOMP
    │
    └─ StompClient ({
        brokerURL: 'ws://localhost:8080/ws-chat',
        reconnect_delay: 5000,
        ...
    })
    │
    ↓
cliente.activate()
    │
    ├─ onConnect: Ejecuta cuando conecta
    │  └─ Envia comando JOIN al backend
    │
    └─ onStompError: Maneja errores
       └─ Muestra error en UI
```

### 4. Envio de Operaciones al Backend

#### JOIN (Unirse a sala)

```
Usuario presiona "Entrar"
    │
    ↓
ChatService.joinRoom()
    │
    ↓
Crea mensaje STOMP
{
  "usuario": "Juan",
  "contenido": ""
}
    │
    ↓
client.publish({
  destination: '/app/chat/General/join',
  body: JSON.stringify(mensaje)
})
    │
    ↓
Backend recibe en ChatController.unirseASala()
    │
    ├─ Agrega usuario a la sala
    ├─ Envia mensaje de bienvenida
    └─ Retransmite lista de usuarios
    │
    ↓
Frontend recibe en /topic/sala/General/usuarios
    │
    └─ Actualiza lista de usuarios en UI
```

#### SEND (Enviar mensaje)

```
Usuario escribe mensaje y presiona Enter/Enviar
    │
    ↓
ChatRoomComponent.enviarMensaje()
    │
    ├─ Valida que mensaje no este vacio
    ├─ Limpia espacios
    └─ Crea objeto ChatMessage
    │
    ↓
ChatService.sendMessage(mensaje)
    │
    ↓
client.publish({
  destination: '/app/chat/General/send',
  body: JSON.stringify(mensaje)
})
    │
    ↓
Backend retransmite a /topic/sala/General
    │
    ↓
Frontend recibe en suscripcion a /topic/sala/General
    │
    └─ ChatRoomComponent.onMessageReceived()
       └─ Agrega mensaje a la lista
          └─ Actualiza UI (scroll al final)
```

#### LEAVE (Abandonar sala)

```
Usuario presiona "Salir" o cierra ventana
    │
    ↓
ChatService.leaveRoom()
    │
    ↓
client.publish({
  destination: '/app/chat/General/leave',
  body: JSON.stringify({ usuario: "Juan" })
})
    │
    ↓
Backend actualiza lista
    │
    ├─ Remueve usuario de la sala
    ├─ Si sala vacia, la elimina
    └─ Retransmite lista actualizada
    │
    ↓
Frontend recibe cambio
    │
    └─ Navega de vuelta a Login
```

#### PRIVATE (Mensaje privado)

```
Usuario hace click en otro usuario
    │
    ↓
Se abre ventana de mensaje privado
    │
    ↓
Usuario escribe y envia
    │
    ↓
ChatService.sendPrivateMessage()
    │
    ↓
client.publish({
  destination: '/app/chat/private',
  body: JSON.stringify({
    usuario: "Juan",
    destinatario: "Maria",
    contenido: "Hola!"
  })
})
    │
    ↓
Backend envia a /queue/private/Maria
    │
    ↓
Frontend recibe (si esta suscrito)
    │
    └─ Muestra notificacion de mensaje privado
```

### 5. Recepcion de Datos del Backend

#### Suscripciones Activas

```
ChatService se suscribe a:

1. /topic/sala/{sala}
   ├─ Recibe mensajes publicos
   ├─ Recibe mensajes de bienvenida (JOIN)
   └─ Recibe mensajes de despedida (LEAVE)
   │
   └─ Callback: onMessageReceived()
      └─ Agrega a lista de mensajes

2. /topic/sala/{sala}/usuarios
   ├─ Recibe lista de usuarios actualizada
   │
   └─ Callback: onUsersUpdated()
      └─ Actualiza lista lateral

3. /queue/private/{usuario}
   ├─ Recibe mensajes privados
   │
   └─ Callback: onPrivateMessage()
      └─ Notifica al usuario
```

## 📊 Modelos de Datos

### message.model.ts

```typescript
export interface ChatMessage {
  id?: string;              // ID unico
  usuario: string;          // Nombre del remitente
  destinatario?: string;    // Para mensajes privados
  sala?: string;            // Sala del mensaje
  contenido: string;        // Contenido
  tipo?: 'NORMAL' | 'JOIN' | 'LEAVE' | 'PRIVADO' | 'USUARIOS_ACTUALIZADOS';
  timestamp?: Date;         // Marca de tiempo
}
```

## 🎨 Componentes Principales

### LoginComponent

**Responsabilidad**: Autenticar usuario y crear sesion

**Datos Recibidos**: Usuario escribe nombre y sala

**Datos Enviados**: 
- Guarda en ChatService
- Navega a /chat

**Validaciones**:
- Usuario no vacio
- Sala no vacia
- Caracteres validos

### ChatRoomComponent

**Responsabilidad**: Interfaz principal del chat

**Datos Recibidos del Backend**:
1. Mensajes en `/topic/sala/{sala}`
2. Usuarios en `/topic/sala/{sala}/usuarios`
3. Mensajes privados en `/queue/private/{usuario}`

**Datos Enviados al Backend**:
1. JOIN al conectar
2. SEND cuando usuario escribe
3. LEAVE al desconectar
4. PRIVATE para mensajes privados

**UI Elements**:
- **Area de mensajes**: Scroll automatico al final
- **Lista de usuarios**: Click para mensaje privado
- **Input de mensaje**: Enter para enviar
- **Selector de emojis**: Agregar emojis al mensaje
- **Boton Salir**: Abandonar sala

## 🔄 Ciclo de Vida de un Mensaje

```
1. Usuario escribe en input
   └─ binding [(ngModel)]="nuevoMensaje"

2. Usuario presiona Enter o click Enviar
   └─ Llama enviarMensaje()

3. Valida y envia via ChatService
   └─ ChatService.sendMessage()

4. ChatService publica a /app/chat/{sala}/send
   └─ Usa client.publish()

5. Backend retransmite a /topic/sala/{sala}
   └─ Todos los clientes reciben

6. Frontend recibe en onMessageReceived()
   └─ mensajes.push(mensaje)

7. Template se actualiza
   └─ *ngFor="let msg of mensajes"

8. View se renderiza
   └─ Se muestra el mensaje en pantalla

9. Scroll se posiciona al final
   └─ scrollToBottom()
```

## 🔗 Suscripciones y Publicaciones

### Publicaciones (Cliente -> Servidor)

| Destino | Operacion | Datos |
|---------|-----------|-------|
| `/app/chat/{sala}/join` | JOIN | {usuario, sala} |
| `/app/chat/{sala}/send` | SEND | {usuario, contenido} |
| `/app/chat/{sala}/leave` | LEAVE | {usuario} |
| `/app/chat/private` | PRIVADO | {usuario, destinatario, contenido} |

### Suscripciones (Servidor -> Cliente)

| Origen | Operacion | Datos |
|--------|-----------|-------|
| `/topic/sala/{sala}` | RECIBIR | Mensajes publicos |
| `/topic/sala/{sala}/usuarios` | USUARIOS | Lista de usuarios |
| `/queue/private/{usuario}` | PRIVADO | Mensajes privados |

## 🚀 Instalacion y Ejecucion

### Instalacion de Dependencias
```bash
npm install
```

### Desarrollo
```bash
npm start
```
Abre http://localhost:4200 automaticamente

### Build Produccion
```bash
npm run build
```

### Testing
```bash
npm test              # Unit tests
npm run e2e          # End-to-end tests
```

## 🛠️ Configuracion

### Environment
```typescript
// src/environment/environment.ts
export const environment = {
  production: false,
  webSocketUrl: 'ws://localhost:8080/ws-chat'
};
```

### Angular Config
```json
// angular.json
{
  "serve": {
    "configurations": {
      "development": {
        "proxyConfig": "proxy.conf.json"
      }
    }
  }
}
```

## 🔍 Debugging

### Ver WebSocket en Dev Tools
```
F12 -> Network -> Filter por WS
Ver frames STOMP enviados y recibidos
```

### Ver Logs en Consola
```
F12 -> Console
ChatService muestra logs de conexion y mensajes
```

### Devtools de Angular
```
npm install -g @angular/devtools
Luego usar extension de Chrome
```

## 📊 Casos de Uso

### Caso 1: Primer Usuario Entra

```
1. Abre http://localhost:4200
2. Escribe "Juan" en usuario
3. Escribe "General" en sala
4. Presiona "Entrar"
5. Ve interfaz de chat vacia (el es el unico)
6. Backend agrega a salas["General"] = {Juan}
```

### Caso 2: Segundo Usuario Entra

```
1. Abre ventana incognita
2. Escribe "Maria" en usuario
3. Escribe "General" en sala
4. Presiona "Entrar"
5. Juan ve actualizada la lista con "Maria"
6. Maria ve mensaje de bienvenida: "Juan se ha unido"
7. Backend agrega a salas["General"] = {Juan, Maria}
```

### Caso 3: Mensajes Privados

```
1. Juan hace click en "Maria" de la lista
2. Se abre chat privado
3. Juan escribe "Hola Maria!"
4. Backend envia a /queue/private/Maria
5. Maria recibe notificacion de mensaje privado
```

## 📱 Responsive Design

- **Desktop**: Interfaz completa con todos los elementos
- **Tablet**: Layout adaptado, sidebar colapsable
- **Mobile**: Solo lista de mensajes y input
- **Emojis**: Se ajustan al ancho de pantalla

## 🔐 Seguridad

- **CORS**: Solo permite http://localhost:4200
- **Validacion**: Todos los inputs se validan
- **XSS**: Angular sanitiza contenido HTML
- **WebSocket**: Validacion en backend de datos recibidos

## 📝 Notas Importantes

1. **Reconexion Automatica**: Si WebSocket se desconecta, intenta reconectar cada 5 segundos
2. **Estado Local**: Usuarios y mensajes se guardan en memoria del navegador
3. **Sin Persistencia**: Al cerrar navegador se pierde todo
4. **Multiples Pestanas**: Cada pestana es una conexion independiente
5. **Mensajes Privados**: Se guardan temporalmente, sin persistencia

## 🔗 Referencias

- Angular: https://angular.dev
- StompJS: https://stomp-js.github.io/stomp-js/
- SockJS: https://github.com/sockjs/sockjs-client
- WebSocket: https://developer.mozilla.org/en-US/docs/Web/API/WebSocket

## 📞 Soporte

Para cambios en:
- Componentes: Ver `components/`
- Servicio: Ver `services/chat.service.ts`
- Modelos: Ver `models/message.model.ts`
- Estilos globales: Ver `styles.css`
