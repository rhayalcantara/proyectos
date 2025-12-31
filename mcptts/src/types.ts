/**
 * Tipos para el servicio de TTS
 */

export interface Voice {
  name: string;
  language: string;
  description: string;
}

export interface SpeakOptions {
  text: string;
  voice?: string;
  rate?: number;  // palabras por minuto (170-220 es normal)
  volume?: number; // 0.0 a 1.0
  outputFile?: string;
}

export interface TTSResult {
  success: boolean;
  message: string;
  error?: string;
}
