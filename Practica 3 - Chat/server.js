/**
 * Servidor UDP-WebSocket Bridge
 * 
 * Propósito: Traduce mensajes UDP (desde clientes Java)
 * a WebSocket (para frontend Angular)
 * 
 * Flujo:
 * Cliente Java UDP:5000 → Node.js:5000 (UDP)
 *                        ↓
 *                   Node.js:8080 (WebSocket)
 *                        ↓
 *                   Frontend Angular (Navegador)
 */

const dgram = require('dgram');
const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const cors = require('cors');

// Configuración
const UDP_PORT = 5000;
const WS_PORT = 8080;
const HOST = 'localhost';

// ============================================================
// GESTIÓN DE ESTADO - SALAS Y USUARIOS
// ============================================================

class SalaManager {
    constructor() {
        this.salas = new Map(); // nombre -> { usuarios: Set, mensajes: [] }
    }

    crearSala(nombre) {
        if (!this.salas.has(nombre)) {
            this.salas.set(nombre, {
                usuarios: new Map(), // usuario -> { nombre, direccion }
                mensajes: []
            });
        }
    }

    unirseASala(nombreSala, usuario, cliente) {
        this.crearSala(nombreSala);
        const sala = this.salas.get(nombreSala);
        sala.usuarios.set(usuario, cliente);
        console.log(`✅ ${usuario} se unió a ${nombreSala}`);
        return sala;
    }

    abandonarSala(nombreSala, usuario) {
        if (this.salas.has(nombreSala)) {
            const sala = this.salas.get(nombreSala);
            sala.usuarios.delete(usuario);
            console.log(`❌ ${usuario} abandonó ${nombreSala}`);
            if (sala.usuarios.size === 0) {
                this.salas.delete(nombreSala);
            }
        }
    }

    agregarMensaje(nombreSala, usuario, contenido) {
        if (this.salas.has(nombreSala)) {
            const sala = this.salas.get(nombreSala);
            sala.mensajes.push({
                usuario,
                contenido,
                timestamp: new Date().toISOString()
            });
        }
    }

    obtenerUsuariosSala(nombreSala) {
        if (this.salas.has(nombreSala)) {
            return Array.from(this.salas.get(nombreSala).usuarios.keys());
        }
        return [];
    }

    obtenerSalaInfo(nombreSala) {
        if (this.salas.has(nombreSala)) {
            const sala = this.salas.get(nombreSala);
            return {
                nombre: nombreSala,
                usuarios: Array.from(sala.usuarios.keys()),
                totalMensajes: sala.mensajes.length
            };
        }
        return null;
    }
}

const salaManager = new SalaManager();

// ============================================================
// 1. SERVIDOR UDP - Escucha clientes Java
// ============================================================

const udpServer = dgram.createSocket('udp4');
const connectedClients = new Map(); // Almacena clientes UDP

udpServer.on('message', (msg, rinfo) => {
    const mensaje = msg.toString().trim();
    console.log(`\n[UDP] Recibido de ${rinfo.address}:${rinfo.port}`);
    console.log(`[UDP] Mensaje: ${mensaje}`);
    
    // Guardar referencia del cliente UDP
    const clientKey = `${rinfo.address}:${rinfo.port}`;
    if (!connectedClients.has(clientKey)) {
        connectedClients.set(clientKey, {
            address: rinfo.address,
            port: rinfo.port
        });
        console.log(`[UDP] Cliente conectado: ${clientKey}`);
    }
    
    // Procesar comando UDP
    procesarComandoUDP(mensaje, clientKey, rinfo);
});

udpServer.on('listening', () => {
    const address = udpServer.address();
    console.log(`
╔════════════════════════════════════════╗
║   Servidor UDP Escuchando              ║
║   Host: ${HOST}                         ║
║   Puerto: ${UDP_PORT}                          ║
╚════════════════════════════════════════╝
    `);
});

udpServer.on('error', (err) => {
    console.error(`[UDP ERROR] ${err.message}`);
    udpServer.close();
});

udpServer.bind(UDP_PORT, HOST);

// ============================================================
// 2. SERVIDOR EXPRESS + WebSocket
// ============================================================

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// Almacena conexiones WebSocket activas con info del usuario
const wsClients = new Map(); // websocket -> { usuario, sala, clientId }

wss.on('connection', (ws) => {
    const clientId = Math.random().toString(36).substr(2, 9);
    wsClients.set(ws, { usuario: null, sala: null, clientId });
    
    console.log(`
[WS] Cliente conectado: ${clientId}
[WS] Total de clientes WebSocket: ${wsClients.size}
    `);
    
    // Enviar confirmación al frontend
    ws.send(JSON.stringify({
        tipo: 'CONEXION_EXITOSA',
        clientId: clientId,
        mensaje: 'Conectado al servidor'
    }));
    
    // Recibir mensajes del frontend
    ws.on('message', (data) => {
        try {
            const mensaje = JSON.parse(data);
            console.log(`[WS] Mensaje recibido de ${clientId}:`, mensaje);
            
            // Procesar comando WebSocket
            procesarComandoWebSocket(mensaje, ws);
        } catch (err) {
            console.error(`[WS ERROR] Error procesando mensaje: ${err.message}`);
        }
    });
    
    ws.on('close', () => {
        const info = wsClients.get(ws);
        if (info && info.usuario && info.sala) {
            salaManager.abandonarSala(info.sala, info.usuario);
            notificarAbandonoSala(info.sala);
        }
        wsClients.delete(ws);
        console.log(`[WS] Cliente desconectado: ${clientId}`);
        console.log(`[WS] Clientes activos: ${wsClients.size}`);
    });
    
    ws.on('error', (err) => {
        console.error(`[WS ERROR] ${err.message}`);
    });
});

// ============================================================
// 3. PROCESAMIENTO DE COMANDOS
// ============================================================

/**
 * Procesa comando UDP desde cliente Java
 * Formato: COMANDO|param1|param2|...
 */
function procesarComandoUDP(comando, clientKey, rinfo) {
    const partes = comando.split('|');
    const tipo = partes[0];

    console.log(`[COMANDO UDP] ${tipo} - ${comando}`);

    if (tipo === 'JOIN') {
        const usuario = partes[1];
        const sala = partes[2];
        salaManager.unirseASala(sala, usuario, {
            address: rinfo.address,
            port: rinfo.port,
            clientKey: clientKey
        });
        
        // Notificar a todos los clientes WebSocket
        notificarAbandonoSala(sala);
        
    } else if (tipo === 'SEND') {
        const usuario = partes[1];
        const sala = partes[2];
        const contenido = partes.slice(3).join('|');
        
        salaManager.agregarMensaje(sala, usuario, contenido);
        
        // Enviar a todos en la sala (contenido ya limpio)
        broadcastASala(sala, {
            tipo: 'NUEVO_MENSAJE',
            usuario: usuario,
            sala: sala,
            contenido: contenido,
            timestamp: new Date().toISOString(),
            privado: false
        });
        
    } else if (tipo === 'LEAVE') {
        const usuario = partes[1];
        const sala = partes[2];
        salaManager.abandonarSala(sala, usuario);
        notificarAbandonoSala(sala);
    } else if (tipo === 'PRIVATE') {
        // PRIVATE|fromUser|toUser|message...
        const fromUser = partes[1];
        const toUser = partes[2];
        const contenido = partes.slice(3).join('|');

        // Opcional: guardar en la sala(s) donde el destinatario esté
        salaManager.salas.forEach((salaObj, salaNombre) => {
            if (salaObj.usuarios.has(toUser)) {
                salaManager.agregarMensaje(salaNombre, fromUser, contenido);
            }
        });

        // Enviar solo al destinatario (WebSocket o UDP)
        // Primero WebSocket
        wsClients.forEach((info, ws) => {
            if (info.usuario === toUser && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({
                    tipo: 'NUEVO_MENSAJE',
                    usuario: fromUser,
                    sala: null,
                    contenido: contenido,
                    timestamp: new Date().toISOString(),
                    privado: true,
                    destinatario: toUser
                }));
            }
        });

            // También enviar copia al emisor (si tiene WebSocket abierto)
            wsClients.forEach((infoSrc, wsSrc) => {
                if (infoSrc.usuario === fromUser && wsSrc.readyState === WebSocket.OPEN) {
                    wsSrc.send(JSON.stringify({
                        tipo: 'NUEVO_MENSAJE',
                        usuario: fromUser,
                        sala: null,
                        contenido: contenido,
                        timestamp: new Date().toISOString(),
                        privado: true,
                        destinatario: toUser
                    }));
                }
            });

        // Después UDP (si existe cliente UDP con clientKey)
        salaManager.salas.forEach((salaObj) => {
            const cliente = salaObj.usuarios.get(toUser);
            if (cliente && cliente.clientKey) {
                const udpCliente = connectedClients.get(cliente.clientKey);
                if (udpCliente) {
                    const buffer = Buffer.from(JSON.stringify({
                        tipo: 'NUEVO_MENSAJE',
                        usuario: fromUser,
                        sala: null,
                        contenido: contenido,
                        timestamp: new Date().toISOString(),
                        privado: true,
                        destinatario: toUser
                    }));
                    udpServer.send(buffer, 0, buffer.length, udpCliente.port, udpCliente.address);
                }
            }
        });

        // Enviar copia al emisor UDP (si viene desde UDP)
        if (clientKey) {
            const udpSender = connectedClients.get(clientKey);
            if (udpSender) {
                const bufferSender = Buffer.from(JSON.stringify({
                    tipo: 'NUEVO_MENSAJE',
                    usuario: fromUser,
                    sala: null,
                    contenido: contenido,
                    timestamp: new Date().toISOString(),
                    privado: true,
                    destinatario: toUser
                }));
                udpServer.send(bufferSender, 0, bufferSender.length, udpSender.port, udpSender.address);
            }
        }
    }
}

/**
 * Procesa comando WebSocket desde frontend Angular
 */
function procesarComandoWebSocket(mensaje, ws) {
    const info = wsClients.get(ws);
    const partes = (mensaje.contenido || '').split('|');
    const tipo = partes[0] || mensaje.tipo || null;

    console.log(`[COMANDO WS] ${tipo} - Usuario: ${info?.usuario}`);

    if (tipo === 'JOIN') {
        const usuario = mensaje.usuario || partes[1];
        const sala = mensaje.sala || partes[2];

        info.usuario = usuario;
        info.sala = sala;

        salaManager.unirseASala(sala, usuario, {
            ws: ws,
            tipo: 'websocket'
        });

        // Enviar confirmación de unión
        ws.send(JSON.stringify({
            tipo: 'UNIDO_SALA',
            sala: sala,
            usuarios: salaManager.obtenerUsuariosSala(sala),
            mensaje: `Te uniste a ${sala}`
        }));

        // Notificar a otros en la sala
        notificarAbandonoSala(sala);

    } else if (tipo === 'SEND') {
        // formato: SEND|fromUser|sala|message...
        const usuario = partes[1] || mensaje.usuario || info.usuario;
        const sala = partes[2] || info.sala || mensaje.sala;
        const contenido = partes.slice(3).join('|');

        if (sala) {
            salaManager.agregarMensaje(sala, usuario, contenido);
            broadcastASala(sala, {
                tipo: 'NUEVO_MENSAJE',
                usuario: usuario,
                sala: sala,
                contenido: contenido,
                timestamp: new Date().toISOString(),
                privado: false
            });
        }

    } else if (tipo === 'LEAVE') {
        if (info.sala && info.usuario) {
            salaManager.abandonarSala(info.sala, info.usuario);
            notificarAbandonoSala(info.sala);
            info.usuario = null;
            info.sala = null;
        }

    } else if (tipo === 'PRIVATE') {
        // formato: PRIVATE|fromUser|toUser|message...
        const fromUser = partes[1] || mensaje.usuario || info.usuario;
        const toUser = partes[2] || mensaje.destinatario;
        const contenido = partes.slice(3).join('|');

        // enviar a destinatario via WebSocket si existe
        wsClients.forEach((infoDest, wsDest) => {
            if (infoDest.usuario === toUser && wsDest.readyState === WebSocket.OPEN) {
                wsDest.send(JSON.stringify({
                    tipo: 'NUEVO_MENSAJE',
                    usuario: fromUser,
                    sala: null,
                    contenido: contenido,
                    timestamp: new Date().toISOString(),
                    privado: true,
                    destinatario: toUser
                }));
            }
        });

        // Enviar copia al emisor WebSocket (confirmación visible para quien envía)
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({
                tipo: 'NUEVO_MENSAJE',
                usuario: fromUser,
                sala: null,
                contenido: contenido,
                timestamp: new Date().toISOString(),
                privado: true,
                destinatario: toUser
            }));
        }

        // enviar via UDP si el destinatario tiene cliente UDP
        salaManager.salas.forEach((salaObj) => {
            const cliente = salaObj.usuarios.get(toUser);
            if (cliente && cliente.clientKey) {
                const udpCliente = connectedClients.get(cliente.clientKey);
                if (udpCliente) {
                    const buffer = Buffer.from(JSON.stringify({
                        tipo: 'NUEVO_MENSAJE',
                        usuario: fromUser,
                        sala: null,
                        contenido: contenido,
                        timestamp: new Date().toISOString(),
                        privado: true,
                        destinatario: toUser
                    }));
                    udpServer.send(buffer, 0, buffer.length, udpCliente.port, udpCliente.address);
                }
            }
        });

        // Enviar copia al emisor UDP (si tiene registro UDP)
        // Buscar cliente UDP del emisor en las salas
        salaManager.salas.forEach((salaObj) => {
            const clienteEmisor = salaObj.usuarios.get(fromUser);
            if (clienteEmisor && clienteEmisor.clientKey) {
                const udpSender = connectedClients.get(clienteEmisor.clientKey);
                if (udpSender) {
                    const bufferSender = Buffer.from(JSON.stringify({
                        tipo: 'NUEVO_MENSAJE',
                        usuario: fromUser,
                        sala: null,
                        contenido: contenido,
                        timestamp: new Date().toISOString(),
                        privado: true,
                        destinatario: toUser
                    }));
                    udpServer.send(bufferSender, 0, bufferSender.length, udpSender.port, udpSender.address);
                }
            }
        });
    }
}

// ============================================================
// 4. FUNCIONES DE RETRANSMISIÓN
// ============================================================

/**
 * Envía mensaje a todos los clientes en una sala
 */
function broadcastASala(nombreSala, data) {
    const usuariosSala = salaManager.salas.get(nombreSala);
    if (!usuariosSala) return;

    const mensaje = JSON.stringify(data);

    // Enviar a clientes WebSocket
    wsClients.forEach((info, ws) => {
        if (info.sala === nombreSala && ws.readyState === WebSocket.OPEN) {
            ws.send(mensaje);
        }
    });

    // Enviar a clientes UDP
    usuariosSala.usuarios.forEach((cliente) => {
        if (cliente.clientKey) {
            const udpCliente = connectedClients.get(cliente.clientKey);
            if (udpCliente) {
                const buffer = Buffer.from(mensaje);
                udpServer.send(buffer, 0, buffer.length, udpCliente.port, udpCliente.address);
            }
        }
    });
}

/**
 * Notifica actualización de usuarios en una sala
 */
function notificarAbandonoSala(nombreSala) {
    const usuarios = salaManager.obtenerUsuariosSala(nombreSala);
    
    const notificacion = JSON.stringify({
        tipo: 'ACTUALIZAR_USUARIOS',
        sala: nombreSala,
        usuarios: usuarios,
        total: usuarios.length
    });

    // Enviar a clientes WebSocket en esa sala
    wsClients.forEach((info, ws) => {
        if (info.sala === nombreSala && ws.readyState === WebSocket.OPEN) {
            ws.send(notificacion);
        }
    });

    // Enviar a clientes UDP en esa sala
    const sala = salaManager.salas.get(nombreSala);
    if (sala) {
        sala.usuarios.forEach((cliente) => {
            if (cliente.clientKey) {
                const udpCliente = connectedClients.get(cliente.clientKey);
                if (udpCliente) {
                    const buffer = Buffer.from(notificacion);
                    udpServer.send(buffer, 0, buffer.length, udpCliente.port, udpCliente.address);
                }
            }
        });
    }
}

// ============================================================
// 5. RUTAS EXPRESS
// ============================================================

app.get('/status', (req, res) => {
    res.json({
        estado: 'activo',
        clientesUDP: connectedClients.size,
        clientesWebSocket: wsClients.size,
        salas: Array.from(salaManager.salas.entries()).map(([nombre, sala]) => ({
            nombre,
            usuarios: Array.from(sala.usuarios.keys()),
            totalMensajes: sala.mensajes.length
        }))
    });
});

app.get('/salas', (req, res) => {
    const salas = Array.from(salaManager.salas.entries()).map(([nombre, sala]) => ({
        nombre,
        usuarios: Array.from(sala.usuarios.keys()),
        totalUsuarios: sala.usuarios.size,
        totalMensajes: sala.mensajes.length
    }));
    res.json(salas);
});

// ============================================================
// 6. INICIAR SERVIDOR
// ============================================================

server.listen(WS_PORT, HOST, () => {
    console.log(`
╔════════════════════════════════════════╗
║   Servidor WebSocket Escuchando        ║
║   Host: ${HOST}                         ║
║   Puerto: ${WS_PORT}                          ║
║   URL: ws://localhost:${WS_PORT}              ║
╚════════════════════════════════════════╝

╔════════════════════════════════════════╗
║        PUENTE UDP ↔ WebSocket          ║
║                                        ║
║  Cliente Java (UDP:5000)               ║
║          ↓                             ║
║  Node.js Puente (UDP:5000 + WS:8080)  ║
║          ↓                             ║
║  Frontend Angular (ws://localhost)     ║
║                                        ║
╚════════════════════════════════════════╝
    `);
});

// Manejo de errores global
process.on('uncaughtException', (err) => {
    console.error('[ERROR GLOBAL]', err);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[UNHANDLED REJECTION]', reason);
});
