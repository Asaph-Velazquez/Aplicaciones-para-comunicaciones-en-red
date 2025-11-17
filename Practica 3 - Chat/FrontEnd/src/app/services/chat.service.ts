import { Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Message, Sala, Usuario } from '../models/message.model';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private ws: WebSocket | null = null;
  private readonly SERVER_URL = 'ws://localhost:8080';
  private conectado = false;

  public usuarioActual = signal<Usuario | null>(null);
  public salasDisponibles = signal<Sala[]>([]);
  public salaActual = signal<Sala | null>(null);
  public mensajes = signal<Message[]>([]);
  public usuariosEnSala = signal<string[]>([]);

  private mensajeRecibido$ = new BehaviorSubject<Message | null>(null);
  private errorConexion$ = new BehaviorSubject<string | null>(null);

  constructor() {}

  conectar(nombreUsuario: string): Promise<boolean> {
    return new Promise((resolve, reject) => {
      try {
        const usuario: Usuario = {
          nombre: nombreUsuario,
          salas: [],
          activo: true
        };
        this.usuarioActual.set(usuario);
        this.ws = new WebSocket(this.SERVER_URL);
        this.ws.onopen = () => {
          this.conectado = true;
          resolve(true);
        };
        this.ws.onmessage = (event) => {
          const data = JSON.parse(event.data);
          this.procesarMensaje(data);
        };
        this.ws.onerror = (event) => {
          reject(new Error('Error conectando'));
        };
        this.ws.onclose = () => {
          this.conectado = false;
        };
      } catch (err) {
        reject(err);
      }
    });
  }

  private procesarMensaje(data: any) {
    console.log('[CHAT SERVICE] Mensaje recibido:', data);

    // Procesar confirmación de conexión
    if (data.tipo === 'CONEXION_EXITOSA') {
      console.log('✅ Conectado al servidor');
      return;
    }

    // Procesar actualización de usuarios en sala
    if (data.tipo === 'ACTUALIZAR_USUARIOS') {
      console.log('👥 Usuarios en sala:', data.usuarios);
      this.usuariosEnSala.set(data.usuarios || []);
      return;
    }

    // Procesar confirmación de unión a sala
    if (data.tipo === 'UNIDO_SALA') {
      console.log('✅ Unido a sala:', data.sala);
      this.usuariosEnSala.set(data.usuarios || []);
      return;
    }

    // Procesar mensaje nuevo (público o privado)
    if (data.tipo === 'NUEVO_MENSAJE') {
      // Defensive: if contenido still contains command prefixes like "SEND|user|sala|msg"
      let contenido = data.contenido || '';
      if (typeof contenido === 'string') {
        const partes = contenido.split('|');
        if (partes.length >= 4 && (partes[0] === 'SEND' || partes[0] === 'PRIVATE')) {
          contenido = partes.slice(3).join('|');
        }
      }

      const isPrivado = data.privado === true || (data.tipo === 'NUEVO_MENSAJE' && data.privado === true);

      // Detectar si el contenido es solo un emoji (o una secuencia de emojis)
      let esSoloEmoji = false;
      try {
        // Unicode property for pictographic (covers most emoji glyphs)
        esSoloEmoji = typeof contenido === 'string' && /^\p{Extended_Pictographic}+(\uFE0F|\u200D\p{Extended_Pictographic})*$/u.test(contenido.trim());
      } catch (e) {
        // Fallback simple heuristic: contenido corto and contains non-word characters (emoji may be non-word)
        esSoloEmoji = typeof contenido === 'string' && contenido.trim().length <= 4 && /[^\w\s]/.test(contenido);
      }

      const tipoMensaje = esSoloEmoji ? 'emoji' : (isPrivado ? 'privado' : 'texto');

      const mensaje: Message = {
        id: Math.random().toString(),
        usuario: data.usuario,
        sala: data.sala || null,
        contenido: contenido,
        tipo: tipoMensaje as Message['tipo'],
        timestamp: data.timestamp ? new Date(data.timestamp) : new Date(),
        destinatario: data.destinatario
      } as Message;

      const usuarioActual = this.usuarioActual()?.nombre;

      // Decidir si agregamos el mensaje a la lista visible
      const esSalaActual = mensaje.sala && mensaje.sala === this.salaActual()?.nombre;
      const esPrivadoParaMi = mensaje.tipo === 'privado' && (mensaje.destinatario === usuarioActual || mensaje.usuario === usuarioActual);

      if (esSalaActual || esPrivadoParaMi) {
        const msgs = this.mensajes();
        this.mensajes.set([...msgs, mensaje]);
        console.log(`💬 Mensaje de ${mensaje.usuario}: ${mensaje.contenido}`);
      }
      return;
    }
  }

  unirseASala(nombreSala: string): void {
    const usuario = this.usuarioActual();
    if (!usuario || !this.conectado || !this.ws) return;

    // Crear el comando en formato esperado por el servidor
    const comando = {
      usuario: usuario.nombre,
      sala: nombreSala,
      contenido: `JOIN|${usuario.nombre}|${nombreSala}`,
      tipo: 'texto',
      timestamp: new Date()
    };

    console.log('📤 Enviando JOIN a sala:', nombreSala);
    this.ws.send(JSON.stringify(comando));

    // Actualizar estado local
    this.salaActual.set({ nombre: nombreSala, usuarios: [], mensajes: [] });
    this.mensajes.set([]);
  }

  enviarMensaje(contenido: string, tipo: 'texto' | 'emoji' | 'sticker' = 'texto'): void {
    const usuario = this.usuarioActual();
    const sala = this.salaActual();
    if (!usuario || !sala || !this.conectado || !this.ws) return;

    // Crear el comando en formato esperado por el servidor
    const comando = {
      usuario: usuario.nombre,
      sala: sala.nombre,
      contenido: `SEND|${usuario.nombre}|${sala.nombre}|${contenido}`,
      tipo: tipo,
      timestamp: new Date()
    };

    console.log('📤 Enviando mensaje a', sala.nombre, ':', contenido);
    this.ws.send(JSON.stringify(comando));
  }

  enviarMensajePrivado(contenido: string, destinatario: string): void {
    const usuario = this.usuarioActual();
    if (!usuario || !this.conectado || !this.ws) return;

    const comando = {
      usuario: usuario.nombre,
      destinatario: destinatario,
      contenido: `PRIVATE|${usuario.nombre}|${destinatario}|${contenido}`,
      tipo: 'privado',
      timestamp: new Date()
    };

    console.log('📤 Enviando mensaje privado a', destinatario);
    this.ws.send(JSON.stringify(comando));
  }

  abandonarSala(): void {
    const usuario = this.usuarioActual();
    const sala = this.salaActual();
    if (!usuario || !sala || !this.conectado || !this.ws) return;

    const comando = {
      usuario: usuario.nombre,
      sala: sala.nombre,
      contenido: `LEAVE|${usuario.nombre}|${sala.nombre}`,
      tipo: 'texto',
      timestamp: new Date()
    };

    console.log('📤 Abandonando sala:', sala.nombre);
    this.ws.send(JSON.stringify(comando));

    this.salaActual.set(null);
    this.mensajes.set([]);
    this.usuariosEnSala.set([]);
  }

  desconectar(): void {
    if (this.ws && this.conectado) {
      this.ws.close();
      this.conectado = false;
      this.usuarioActual.set(null);
      this.salaActual.set(null);
      this.mensajes.set([]);
    }
  }

  get estaConectado(): boolean {
    return this.conectado;
  }

  get getMensajeRecibido() {
    return this.mensajeRecibido$.asObservable();
  }
}
