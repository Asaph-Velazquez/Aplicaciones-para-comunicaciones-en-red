import { Component, signal, effect, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { Message } from '../../models/message.model';

@Component({
  selector: 'app-chat-room',
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-room.component.html',
  styleUrl: './chat-room.component.css'
})
export class ChatRoomComponent {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  // Señales
  nombreSala = signal('');
  nuevoMensaje = signal('');
  mostrarUnirse = signal(true);
  mostrarUsuarios = signal(true);
  usuarioSeleccionado = signal<string | null>(null);
  modoPrivado = signal(false);

  // Emojis disponibles
  emojis = ['😀', '😂', '❤️', '👍', '🎉', '🔥', '💯', '✨', '👋', '🙌'];
  mostrarEmojis = signal(false);

  // Stickers disponibles
  stickers = ['🎨', '🎭', '🎪', '🎬', '🎮', '🎯', '🎲', '🎰', '🎳', '⚽', '🏀', '🏈', '⚾', '🎾', '🏐', '🏉', '🎱', '🏓', '🏸', '🥊'];
  mostrarStickers = signal(false);

  // Audio recording
  mediaRecorder: MediaRecorder | null = null;
  audioChunks: Blob[] = [];
  grabandoAudio = signal(false);
  tiempoGrabacion = signal(0);
  intervaloGrabacion: any = null;

  constructor(
    public chatService: ChatService,
    private router: Router
  ) {
    // Verificar si el usuario está autenticado
    if (!this.chatService.usuarioActual()) {
      this.router.navigate(['/']);
    }

    // Auto-scroll cuando lleguen nuevos mensajes
    effect(() => {
      const mensajes = this.chatService.mensajes();
      if (mensajes.length > 0) {
        setTimeout(() => this.scrollToBottom(), 100);
      }
    });
  }

  unirseASala(): void {
    const nombre = this.nombreSala().trim();
    if (!nombre) return;

    this.chatService.unirseASala(nombre);
    this.mostrarUnirse.set(false);
    this.nombreSala.set('');
  }

  abandonarSala(): void {
    this.chatService.abandonarSala();
    this.mostrarUnirse.set(true);
    this.usuarioSeleccionado.set(null);
    this.modoPrivado.set(false);
  }

  enviarMensaje(): void {
    const mensaje = this.nuevoMensaje().trim();
    if (!mensaje) return;

    console.log('🔍 [DEBUG] Enviando mensaje - Modo privado:', this.modoPrivado(), '- Usuario seleccionado:', this.usuarioSeleccionado());

    if (this.modoPrivado() && this.usuarioSeleccionado()) {
      console.log('✅ Enviando como PRIVADO a:', this.usuarioSeleccionado());
      this.chatService.enviarMensajePrivado(mensaje, this.usuarioSeleccionado()!);
    } else {
      console.log('✅ Enviando como PÚBLICO');
      this.chatService.enviarMensaje(mensaje);
    }

    this.nuevoMensaje.set('');
    this.mostrarEmojis.set(false);
  }

  enviarEmoji(emoji: string): void {
    if (this.modoPrivado() && this.usuarioSeleccionado()) {
      // Enviar emoji en modo privado
      this.chatService.enviarMensajePrivado(emoji, this.usuarioSeleccionado()!, 'emoji');
    } else {
      // Enviar emoji en modo público
      this.chatService.enviarMensaje(emoji, 'emoji');
    }
    this.mostrarEmojis.set(false);
  }

  toggleEmojis(): void {
    this.mostrarEmojis.update(v => !v);
    if (this.mostrarEmojis()) {
      this.mostrarStickers.set(false);
    }
  }

  toggleStickers(): void {
    this.mostrarStickers.update(v => !v);
    if (this.mostrarStickers()) {
      this.mostrarEmojis.set(false);
    }
  }

  enviarSticker(sticker: string): void {
    if (this.modoPrivado() && this.usuarioSeleccionado()) {
      this.chatService.enviarMensajePrivado(sticker, this.usuarioSeleccionado()!, 'sticker');
    } else {
      this.chatService.enviarMensaje(sticker, 'sticker');
    }
    this.mostrarStickers.set(false);
  }

  async iniciarGrabacion(): Promise<void> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      
      this.audioChunks = [];
      this.mediaRecorder = new MediaRecorder(stream);
      
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
        await this.enviarAudio(audioBlob);
        
        // Detener todas las pistas de audio
        stream.getTracks().forEach(track => track.stop());
      };

      this.mediaRecorder.start();
      this.grabandoAudio.set(true);
      this.tiempoGrabacion.set(0);

      // Contador de tiempo
      this.intervaloGrabacion = setInterval(() => {
        this.tiempoGrabacion.update(t => t + 1);
        
        // Límite de 60 segundos
        if (this.tiempoGrabacion() >= 60) {
          this.detenerGrabacion();
        }
      }, 1000);

    } catch (error) {
      console.error('Error al acceder al micrófono:', error);
      alert('No se pudo acceder al micrófono. Por favor, verifica los permisos.');
    }
  }

  detenerGrabacion(): void {
    if (this.mediaRecorder && this.grabandoAudio()) {
      this.mediaRecorder.stop();
      this.grabandoAudio.set(false);
      
      if (this.intervaloGrabacion) {
        clearInterval(this.intervaloGrabacion);
        this.intervaloGrabacion = null;
      }
    }
  }

  cancelarGrabacion(): void {
    if (this.mediaRecorder && this.grabandoAudio()) {
      this.mediaRecorder.stop();
      this.audioChunks = [];
      this.grabandoAudio.set(false);
      this.tiempoGrabacion.set(0);
      
      if (this.intervaloGrabacion) {
        clearInterval(this.intervaloGrabacion);
        this.intervaloGrabacion = null;
      }
    }
  }

  async enviarAudio(audioBlob: Blob): Promise<void> {
    // Convertir blob a base64
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64Audio = reader.result as string;
      
      if (this.modoPrivado() && this.usuarioSeleccionado()) {
        this.chatService.enviarAudioPrivado(base64Audio, this.usuarioSeleccionado()!);
      } else {
        this.chatService.enviarAudio(base64Audio);
      }
    };
    reader.readAsDataURL(audioBlob);
  }

  formatearTiempoGrabacion(): string {
    const tiempo = this.tiempoGrabacion();
    const minutos = Math.floor(tiempo / 60);
    const segundos = tiempo % 60;
    return `${minutos}:${segundos.toString().padStart(2, '0')}`;
  }

  seleccionarUsuarioPrivado(usuario: string): void {
    const usuarioActual = this.chatService.usuarioActual();
    if (usuario === usuarioActual?.nombre) return;

    this.usuarioSeleccionado.set(usuario);
    this.modoPrivado.set(true);
  }

  cancelarModoPrivado(): void {
    this.usuarioSeleccionado.set(null);
    this.modoPrivado.set(false);
  }

  esMensajePropio(mensaje: Message): boolean {
    return mensaje.usuario === this.chatService.usuarioActual()?.nombre;
  }

  esMensajePrivado(mensaje: Message): boolean {
    const usuario = this.chatService.usuarioActual()?.nombre;
    // Un mensaje es privado si:
    // 1. Su tipo es 'privado' O
    // 2. Tiene destinatario (incluso si es emoji, sticker o audio)
    return (mensaje.tipo === 'privado' || (!!mensaje.destinatario && mensaje.destinatario !== '')) && 
           (mensaje.usuario === usuario || mensaje.destinatario === usuario);
  }

  obtenerMensajesFiltrados(): Message[] {
    const mensajes = this.chatService.mensajes();
    const usuario = this.chatService.usuarioActual()?.nombre;
    const salaActual = this.chatService.salaActual()?.nombre;

    if (this.modoPrivado() && this.usuarioSeleccionado()) {
      // En modo privado, mostrar solo mensajes privados entre estos dos usuarios
      const otroUsuario = this.usuarioSeleccionado()!;
      return mensajes.filter(msg => 
        // Debe tener destinatario (es privado)
        msg.destinatario &&
        msg.destinatario !== '' &&
        // Y debe ser entre los dos usuarios en conversación
        ((msg.usuario === usuario && msg.destinatario === otroUsuario) ||
         (msg.usuario === otroUsuario && msg.destinatario === usuario))
      );
    } else {
      // En modo normal, mostrar solo mensajes públicos de la sala actual
      // Excluir TODOS los mensajes que tengan destinatario (son privados)
      return mensajes.filter(msg => {
        const tieneDestinatario = msg.destinatario && msg.destinatario !== '';
        const esDeLaSala = msg.sala === salaActual;
        
        // Debug
        if (tieneDestinatario) {
          console.log('🔒 Mensaje privado filtrado:', msg.usuario, '->', msg.destinatario, 'Tipo:', msg.tipo);
        }
        
        // Mostrar solo si NO tiene destinatario Y es de la sala actual
        return !tieneDestinatario && esDeLaSala;
      });
    }
  }

  formatearHora(fecha: Date): string {
    return new Date(fecha).toLocaleTimeString('es-MX', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }

  cerrarSesion(): void {
    this.chatService.desconectar();
    this.router.navigate(['/']);
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = 
          this.messagesContainer.nativeElement.scrollHeight;
      }
    } catch(err) { 
      console.error('Error al hacer scroll:', err);
    }
  }
}
