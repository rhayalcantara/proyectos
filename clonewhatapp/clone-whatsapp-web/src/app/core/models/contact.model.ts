export interface Contact {
  id: string;
  contactoUsuarioId: string;
  numeroTelefono: string;
  nombre: string;
  nombrePersonalizado?: string;
  fotoPerfil?: string;
  estado: string;
  bloqueado: boolean;
  estaEnLinea: boolean;
  ultimaConexion: Date;
}

export interface AddContactRequest {
  numeroTelefono: string;
  nombrePersonalizado?: string;
}

export interface UpdateContactRequest {
  nombrePersonalizado?: string;
  bloqueado?: boolean;
}
