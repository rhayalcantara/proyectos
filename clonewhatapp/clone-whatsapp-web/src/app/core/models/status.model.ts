export interface Status {
  id: string;
  usuarioId: string;
  usuarioNombre: string;
  usuarioFoto?: string;
  tipo: 'Texto' | 'Imagen';
  contenido?: string;
  urlArchivo?: string;
  colorFondo?: string;
  fechaCreacion: Date;
  fechaExpiracion: Date;
  totalVistas: number;
  vioPorMi: boolean;
}

export interface StatusViewer {
  id: string;
  usuarioId: string;
  usuarioNombre: string;
  usuarioFoto?: string;
  fechaVista: Date;
}

export interface ContactStatuses {
  usuarioId: string;
  usuarioNombre: string;
  usuarioFoto?: string;
  estados: Status[];
  todosVistos: boolean;
  ultimaActualizacion: Date;
}

export interface MyStatuses {
  estados: Status[];
  totalVistas: number;
}

export interface CreateTextStatusRequest {
  contenido: string;
  colorFondo?: string;
}
