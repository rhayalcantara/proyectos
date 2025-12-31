import { User } from './user.model';
import { Message } from './message.model';

export interface Chat {
  id: string;
  tipo: 'Individual' | 'Grupo';
  fechaCreacion: Date;
  ultimaActividad?: Date;
  nombreGrupo?: string;
  imagenGrupo?: string;
  descripcionGrupo?: string;
  otroParticipante?: User;
  ultimoMensaje?: Message;
  mensajesNoLeidos: number;
  participantes: Participant[];
  silenciado: boolean;
  silenciadoHasta?: Date | null;
  archivado: boolean;
}

export interface MuteChatRequest {
  silenciar: boolean;
  duracion?: '8h' | '1w' | 'always' | null;
}

export interface ArchiveChatRequest {
  archivar: boolean;
}

export interface Participant {
  usuarioId: string;
  nombre: string;
  fotoPerfil?: string;
  rol: 'Admin' | 'Participante';
  estaEnLinea: boolean;
}

export interface CreateIndividualChatRequest {
  contactoId: string;
}

export interface CreateGroupRequest {
  nombre: string;
  descripcion?: string;
  participantesIds: string[];
}
