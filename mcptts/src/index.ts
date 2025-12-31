#!/usr/bin/env node

/**
 * MCP Server para Text-to-Speech usando el comando say de macOS
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { speakText, stopSpeech, listVoices } from './tts-service.js';

/**
 * Crear el servidor MCP
 */
const server = new Server(
  {
    name: 'mcptts',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

/**
 * Handler para listar herramientas disponibles
 */
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: 'speak_text',
        description:
          'Convierte texto a voz usando el TTS nativo de macOS. Reproduce el texto en voz alta usando la voz especificada en español.',
        inputSchema: {
          type: 'object',
          properties: {
            text: {
              type: 'string',
              description: 'El texto que se leerá en voz alta',
            },
            voice: {
              type: 'string',
              description:
                'Nombre de la voz a usar (Monica, Paulina, Juan, Diego). Por defecto: Monica',
              default: 'Monica',
            },
            rate: {
              type: 'number',
              description:
                'Velocidad de habla en palabras por minuto (80-500). Por defecto: 200',
              default: 200,
            },
            outputFile: {
              type: 'string',
              description: 'Ruta opcional para guardar el audio en un archivo AIFF',
            },
          },
          required: ['text'],
        },
      },
      {
        name: 'list_voices',
        description:
          'Lista todas las voces en español disponibles en el sistema macOS',
        inputSchema: {
          type: 'object',
          properties: {},
        },
      },
      {
        name: 'stop_speech',
        description: 'Detiene cualquier reproducción de voz en curso',
        inputSchema: {
          type: 'object',
          properties: {},
        },
      },
    ],
  };
});

/**
 * Handler para ejecutar herramientas
 */
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case 'speak_text': {
        const { text, voice, rate, outputFile } = args as {
          text: string;
          voice?: string;
          rate?: number;
          outputFile?: string;
        };

        const result = await speakText({ text, voice, rate, outputFile });

        return {
          content: [
            {
              type: 'text',
              text: result.success
                ? result.message
                : `Error: ${result.error || result.message}`,
            },
          ],
        };
      }

      case 'list_voices': {
        const voices = await listVoices();
        const voiceList = voices
          .map((v) => `- ${v.name} (${v.language}): ${v.description}`)
          .join('\n');

        return {
          content: [
            {
              type: 'text',
              text: `Voces en español disponibles:\n\n${voiceList}`,
            },
          ],
        };
      }

      case 'stop_speech': {
        const result = await stopSpeech();

        return {
          content: [
            {
              type: 'text',
              text: result.message,
            },
          ],
        };
      }

      default:
        throw new Error(`Herramienta desconocida: ${name}`);
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Error desconocido';
    return {
      content: [
        {
          type: 'text',
          text: `Error al ejecutar ${name}: ${errorMessage}`,
        },
      ],
      isError: true,
    };
  }
});

/**
 * Iniciar el servidor
 */
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);

  console.error('MCP TTS Server iniciado');
}

main().catch((error) => {
  console.error('Error al iniciar el servidor:', error);
  process.exit(1);
});
