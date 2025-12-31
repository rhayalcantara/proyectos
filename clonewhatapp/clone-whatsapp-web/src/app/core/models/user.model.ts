export interface User {
  id: string;
  numeroTelefono: string;
  nombre: string;
  fotoPerfil?: string;
  estado: string;
  ultimaConexion: Date;
  estaEnLinea: boolean;
}

export interface AuthResponse {
  id: string;
  numeroTelefono: string;
  nombre: string;
  fotoPerfil?: string;
  token: string;
  tokenExpiration: Date;
}

export interface LoginRequest {
  numeroTelefono: string;
  password: string;
}

export interface RegisterRequest {
  numeroTelefono: string;
  nombre: string;
  password: string;
}

export interface UpdateProfileRequest {
  nombre?: string;
  estado?: string;
}

export interface BlockStatus {
  estaBloqueado: boolean;
  meBloquearon: boolean;
}

export interface BlockedUser {
  id: string;
  numeroTelefono: string;
  nombre: string;
  fotoPerfil?: string;
  estado: string;
  ultimaConexion: Date;
  estaEnLinea: boolean;
}
