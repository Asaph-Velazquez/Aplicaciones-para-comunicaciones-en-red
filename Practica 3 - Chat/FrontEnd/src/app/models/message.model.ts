export interface Message {
  id?: string;
  usuario: string;
  sala?: string;
  contenido: string;
  tipo: 'texto' | 'emoji' | 'sticker' | 'audio' | 'privado';
  timestamp: Date;
  destinatario?: string;
  audioData?: string; 
  stickerUrl?: string; 
}

export interface Usuario {
  nombre: string;
  salas: string[];
  activo: boolean;
}

export interface Sala {
  nombre: string;
  usuarios: string[];
  mensajes?: Message[];
}
