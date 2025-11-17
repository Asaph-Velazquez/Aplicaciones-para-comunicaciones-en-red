import { Component, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ChatService } from '../../services/chat.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  nombreUsuario = signal('');
  errorMensaje = signal('');
  cargando = signal(false);

  constructor(
    private chatService: ChatService,
    private router: Router
  ) {}

  async iniciarSesion(): Promise<void> {
    const nombre = this.nombreUsuario().trim();
    
    console.log('🔍 Intentando iniciar sesión con:', nombre);
    
    if (!nombre) {
      this.errorMensaje.set('Por favor ingrese un nombre de usuario');
      return;
    }

    if (nombre.length < 3) {
      this.errorMensaje.set('El nombre debe tener al menos 3 caracteres');
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');

    try {
      console.log('🔌 Conectando al servidor WebSocket...');
      await this.chatService.conectar(nombre);
      console.log('✅ Conexión exitosa, navegando al chat...');
      this.router.navigate(['/chat']);
    } catch (error) {
      console.error('❌ Error al conectar:', error);
      this.errorMensaje.set('Error al conectar con el servidor');
      console.error(error);
    } finally {
      this.cargando.set(false);
    }
  }

  actualizarNombre(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.nombreUsuario.set(input.value);
    console.log('📝 Nombre actualizado:', input.value);
    this.limpiarError();
  }

  limpiarError(): void {
    this.errorMensaje.set('');
  }
}
