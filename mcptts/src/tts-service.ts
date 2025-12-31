/**
 * Servicio de Text-to-Speech usando el comando say de macOS
 */

import { exec } from 'child_process';
import { promisify } from 'util';
import type { Voice, SpeakOptions, TTSResult } from './types.js';

const execAsync = promisify(exec);

/**
 * Voces en español disponibles en macOS
 */
export const SPANISH_VOICES: Voice[] = [
  { name: 'Monica', language: 'es-MX', description: 'Español mexicano (femenina)' },
  { name: 'Paulina', language: 'es-MX', description: 'Español mexicano (femenina)' },
  { name: 'Juan', language: 'es-ES', description: 'Español de España (masculina)' },
  { name: 'Diego', language: 'es-AR', description: 'Español argentino (masculina)' },
];

/**
 * Sanitiza el texto para evitar inyección de comandos
 */
function sanitizeText(text: string): string {
  // Escapa comillas dobles y backslashes
  return text.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
}

/**
 * Divide texto largo en chunks más pequeños respetando límites de oraciones
 */
function splitTextIntoChunks(text: string, maxChunkSize: number = 500): string[] {
  // Si el texto es menor al límite, retornarlo completo
  if (text.length <= maxChunkSize) {
    return [text];
  }

  const chunks: string[] = [];
  // Dividir por oraciones (punto, exclamación, interrogación seguidos de espacio o fin de texto)
  const sentences = text.split(/([.!?]+\s+|[.!?]+$)/);

  let currentChunk = '';

  for (let i = 0; i < sentences.length; i++) {
    const sentence = sentences[i];

    // Si agregar esta oración excede el límite y ya tenemos contenido
    if (currentChunk.length + sentence.length > maxChunkSize && currentChunk.length > 0) {
      chunks.push(currentChunk.trim());
      currentChunk = sentence;
    } else {
      currentChunk += sentence;
    }
  }

  // Agregar el último chunk si tiene contenido
  if (currentChunk.trim().length > 0) {
    chunks.push(currentChunk.trim());
  }

  return chunks;
}

/**
 * Obtiene todas las voces disponibles en el sistema
 */
export async function listVoices(): Promise<Voice[]> {
  try {
    const { stdout } = await execAsync('say -v "?"');
    const lines = stdout.split('\n');
    const voices: Voice[] = [];

    for (const line of lines) {
      if (line.trim()) {
        const match = line.match(/^(\S+)\s+(\S+)\s+(.+)$/);
        if (match) {
          const [, name, language, description] = match;
          // Filtrar solo voces en español
          if (language.startsWith('es_') || language.startsWith('es-')) {
            voices.push({
              name,
              language: language.replace('_', '-'),
              description: description.trim(),
            });
          }
        }
      }
    }

    return voices.length > 0 ? voices : SPANISH_VOICES;
  } catch (error) {
    console.error('Error al listar voces:', error);
    return SPANISH_VOICES;
  }
}

/**
 * Convierte texto a voz usando el comando say de macOS
 */
export async function speakText(options: SpeakOptions): Promise<TTSResult> {
  try {
    const { text, voice = 'Monica', rate, volume, outputFile } = options;

    if (!text || text.trim().length === 0) {
      return {
        success: false,
        message: 'El texto está vacío',
        error: 'El texto no puede estar vacío',
      };
    }

    // Dividir el texto en chunks si es muy largo
    const chunks = splitTextIntoChunks(text, 500);

    // Si hay un archivo de salida, combinar todos los chunks en un solo archivo
    if (outputFile) {
      const sanitizedText = sanitizeText(text);
      let command = `say -v "${voice}"`;

      if (rate) {
        command += ` -r ${Math.max(80, Math.min(500, rate))}`;
      }

      command += ` -o "${sanitizeText(outputFile)}" "${sanitizedText}"`;

      await execAsync(command, { timeout: 120000 });

      return {
        success: true,
        message: `Audio guardado en: ${outputFile}`,
      };
    }

    // Para reproducción en voz alta, procesar cada chunk secuencialmente
    let chunkNumber = 1;
    for (const chunk of chunks) {
      const sanitizedChunk = sanitizeText(chunk);
      let command = `say -v "${voice}"`;

      if (rate) {
        command += ` -r ${Math.max(80, Math.min(500, rate))}`;
      }

      command += ` "${sanitizedChunk}"`;

      // Ejecutar el comando con timeout extendido
      await execAsync(command, { timeout: 60000 });

      chunkNumber++;
    }

    return {
      success: true,
      message: chunks.length > 1
        ? `Audio reproducido exitosamente (${chunks.length} fragmentos)`
        : 'Audio reproducido exitosamente',
    };
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Error desconocido';
    return {
      success: false,
      message: 'Error al reproducir el audio',
      error: errorMessage,
    };
  }
}

/**
 * Detiene cualquier reproducción en curso
 */
export async function stopSpeech(): Promise<TTSResult> {
  try {
    await execAsync('killall say');
    return {
      success: true,
      message: 'Reproducción detenida',
    };
  } catch (error) {
    return {
      success: false,
      message: 'No hay reproducción en curso',
      error: 'No se encontró ningún proceso de reproducción',
    };
  }
}
