
## Soluciones para Claude Desktop en Mac:

### 1. **MCP Server con TTS nativo de macOS**
La mejor opción sería crear un MCP server que use el comando `say` de macOS:

```typescript
// mcp-mac-tts/src/index.ts
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

const server = new Server({
  name: 'mac-tts',
  version: '1.0.0',
});

server.setRequestHandler('tools/list', async () => ({
  tools: [{
    name: 'speak_text',
    description: 'Convierte texto a voz usando el TTS nativo de macOS',
    inputSchema: {
      type: 'object',
      properties: {
        text: { type: 'string', description: 'Texto a leer' },
        voice: { type: 'string', default: 'Monica', description: 'Voz en español' }
      },
      required: ['text']
    }
  }]
}));

server.setRequestHandler('tools/call', async (request) => {
  if (request.params.name === 'speak_text') {
    const { text, voice = 'Monica' } = request.params.arguments;
    await execAsync(`say -v ${voice} "${text.replace(/"/g, '\\"')}"`);
    return { content: [{ type: 'text', text: 'Audio reproducido' }] };
  }
});

const transport = new StdioServerTransport();
server.connect(transport);
```

### 2. **Configuración en Claude Desktop:**

```json
// ~/Library/Application Support/Claude/claude_desktop_config.json
{
  "mcpServers": {
    "mac-tts": {
      "command": "node",
      "args": ["/ruta/a/tu/mcp-mac-tts/build/index.js"]
    }
  }
}
```

### 3. **Voces en español disponibles en Mac:**
```bash
# Ver voces disponibles
say -v "?"

# Voces en español:
# - Monica (español mexicano)
# - Paulina (español mexicano)
# - Juan (español español)
# - Diego (español argentino)
```

## Problema actual:
El desafío es que necesitarías **llamar manualmente** la herramienta después de cada respuesta mía, porque Claude Desktop no tiene un hook automático para capturar mis respuestas y reproducirlas.

### Alternativa más automática:
Podrías crear un script de macOS que:
1. Monitoree el portapapeles
2. Cuando copies mi respuesta, automáticamente la lea

¿Te gustaría que te ayude a implementar el MCP server con TTS de macOS? Es la solución más integrada con Claude Desktop.