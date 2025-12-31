export interface Message {
  id: string;
  chatId: string;
  remitenteId: string;
  remitenteNombre?: string;
  contenido?: string;
  tipo: 'Texto' | 'Imagen' | 'Video' | 'Audio' | 'Documento' | 'Ubicacion' | 'Contacto' | 'Sistema';
  urlArchivo?: string;
  nombreArchivo?: string;
  tamanoArchivo?: number;
  duracionSegundos?: number;
  mensajeRespondidoId?: string;
  mensajeRespondido?: Message;
  fechaEnvio: Date;
  eliminado: boolean;
  eliminadoParaTodos: boolean;
  editado: boolean;
  estado: 'Enviado' | 'Entregado' | 'Leido';
}

export interface SendMessageRequest {
  chatId: string;
  contenido?: string;
  tipo: string;
  mensajeRespondidoId?: string;
}

export interface ReplyToMessage {
  id: string;
  contenido?: string;
  remitenteNombre?: string;
  tipo: string;
}

export interface MessageSentEvent {
  mensajeId: string;
  chatId: string;
  mensaje: Message;
}

export interface TypingEvent {
  chatId: string;
  usuarioId: string;
  nombreUsuario: string;
  estaEscribiendo: boolean;
}

export interface MessageStatusEvent {
  messageId: string;
  status: string;
  userId?: string;
}

export interface MessagesReadEvent {
  chatId: string;
  messageIds: string[];
  userId: string;
}
