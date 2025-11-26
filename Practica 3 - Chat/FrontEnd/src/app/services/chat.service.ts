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
      let contenido = data.contenido || '';
      
      const esAudioBase64 = typeof contenido === 'string' && contenido.startsWith('data:audio');
      
      if (typeof contenido === 'string' && !esAudioBase64) {
        const partes = contenido.split('|');
        if (partes.length >= 4 && (partes[0] === 'SEND' || partes[0] === 'PRIVATE')) {
          contenido = partes.slice(3).join('|');
        }
      }

      const isPrivado = data.privado === true || (data.tipo === 'NUEVO_MENSAJE' && data.privado === true);

      // Detectar si el contenido es solo un emoji (o una secuencia de emojis)
      let esSoloEmoji = false;
      if (!esAudioBase64) {
        try {
          // Unicode property for pictographic (covers most emoji glyphs)
          esSoloEmoji = typeof contenido === 'string' && /^\p{Extended_Pictographic}+(\uFE0F|\u200D\p{Extended_Pictographic})*$/u.test(contenido.trim());
        } catch (e) {
          // Fallback simple heuristic: contenido corto and contains non-word characters (emoji may be non-word)
          esSoloEmoji = typeof contenido === 'string' && contenido.trim().length <= 4 && /[^\w\s]/.test(contenido);
        }
      }

      // Determinar tipo de mensaje basándose en múltiples factores
      let tipoMensaje: Message['tipo'];
      if (data.tipoMensaje === 'AUDIO' || esAudioBase64) {
        tipoMensaje = 'audio';
      } else if (data.tipoMensaje === 'STICKER') {
        tipoMensaje = 'sticker';
      } else if (data.tipoMensaje === 'EMOJI' || esSoloEmoji) {
        tipoMensaje = 'emoji';
      } else if (isPrivado || data.tipoMensaje === 'PRIVADO' || data.destinatario) {
        tipoMensaje = 'privado';
      } else {
        tipoMensaje = 'texto';
      }

      const mensaje: Message = {
        id: Math.random().toString(),
        usuario: data.usuario,
        sala: data.sala || null,
        contenido: contenido,
        tipo: tipoMensaje,
        timestamp: data.timestamp ? new Date(data.timestamp) : new Date(),
        destinatario: data.destinatario,
        audioData: tipoMensaje === 'audio' ? contenido : undefined,
        stickerUrl: tipoMensaje === 'sticker' ? contenido : undefined
      } as Message;

      const usuarioActual = this.usuarioActual()?.nombre;
      const esSalaActual = mensaje.sala && mensaje.sala === this.salaActual()?.nombre;
      
      const esPrivadoParaMi = mensaje.destinatario && 
                              (mensaje.destinatario === usuarioActual || mensaje.usuario === usuarioActual);

      if (esSalaActual || esPrivadoParaMi) {
        const msgs = this.mensajes();
        this.mensajes.set([...msgs, mensaje]);
        console.log(`💬 Mensaje de ${mensaje.usuario}: ${mensaje.contenido.substring(0, 50)} (Tipo: ${mensaje.tipo}, Privado: ${!!mensaje.destinatario})`);
      }
      return;
    }
  }

  unirseASala(nombreSala: string): void {
    const usuario = this.usuarioActual();
    if (!usuario || !this.conectado || !this.ws) return;

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

    let contenidoFinal = contenido;
    if (tipo === 'texto' || tipo === 'emoji') {
      contenidoFinal = `SEND|${usuario.nombre}|${sala.nombre}|${contenido}`;
    }

    const comando = {
      usuario: usuario.nombre,
      sala: sala.nombre,
      contenido: contenidoFinal,
      tipo: tipo,
      timestamp: new Date()
    };

    console.log('📤 Enviando mensaje a', sala.nombre, '- Tipo:', tipo);
    this.ws.send(JSON.stringify(comando));
  }

  enviarAudio(audioBase64: string): void {
    const usuario = this.usuarioActual();
    const sala = this.salaActual();
    if (!usuario || !sala || !this.conectado || !this.ws) return;

    const comando = {
      usuario: usuario.nombre,
      sala: sala.nombre,
      contenido: audioBase64,
      tipo: 'audio',
      timestamp: new Date()
    };

    console.log('📤 Enviando audio a', sala.nombre);
    this.ws.send(JSON.stringify(comando));
  }

  enviarAudioPrivado(audioBase64: string, destinatario: string): void {
    const usuario = this.usuarioActual();
    if (!usuario || !this.conectado || !this.ws) return;

    const comando = {
      usuario: usuario.nombre,
      destinatario: destinatario,
      contenido: audioBase64,
      tipo: 'audio',
      timestamp: new Date()
    };

    console.log('📤 Enviando audio privado a', destinatario);
    this.ws.send(JSON.stringify(comando));
  }

  enviarMensajePrivado(contenido: string, destinatario: string, tipo: 'texto' | 'emoji' | 'sticker' = 'texto'): void {
    const usuario = this.usuarioActual();
    if (!usuario || !this.conectado || !this.ws) return;

    let contenidoFinal = contenido;
    if (tipo === 'texto' || tipo === 'emoji') {
      contenidoFinal = `PRIVATE|${usuario.nombre}|${destinatario}|${contenido}`;
    }

    const comando = {
      usuario: usuario.nombre,
      destinatario: destinatario,
      contenido: contenidoFinal,
      tipo: tipo,
      timestamp: new Date()
    };

    console.log('📤 [PRIVADO] Enviando a', destinatario, '- Tipo:', tipo, '- Comando:', comando);
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
