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

    if (this.modoPrivado() && this.usuarioSeleccionado()) {
      this.chatService.enviarMensajePrivado(mensaje, this.usuarioSeleccionado()!);
    } else {
      this.chatService.enviarMensaje(mensaje);
    }

    this.nuevoMensaje.set('');
    this.mostrarEmojis.set(false);
  }

  enviarEmoji(emoji: string): void {
    this.chatService.enviarMensaje(emoji, 'emoji');
    this.mostrarEmojis.set(false);
  }

  toggleEmojis(): void {
    this.mostrarEmojis.update(v => !v);
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
    return mensaje.tipo === 'privado' && 
           (mensaje.usuario === usuario || mensaje.destinatario === usuario);
  }

  obtenerMensajesFiltrados(): Message[] {
    const mensajes = this.chatService.mensajes();
    const usuario = this.chatService.usuarioActual()?.nombre;

    if (this.modoPrivado() && this.usuarioSeleccionado()) {
      // En modo privado, mostrar solo mensajes privados entre estos dos usuarios
      const otroUsuario = this.usuarioSeleccionado()!;
      return mensajes.filter(msg => 
        msg.tipo === 'privado' &&
        ((msg.usuario === usuario && msg.destinatario === otroUsuario) ||
         (msg.usuario === otroUsuario && msg.destinatario === usuario))
      );
    } else {
      // En modo normal, mostrar solo mensajes públicos
      return mensajes.filter(msg => msg.tipo !== 'privado');
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
