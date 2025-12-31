# MCP TTS - Text-to-Speech para Claude Desktop en macOS

MCP Server que permite a Claude Desktop convertir texto a voz usando el motor TTS nativo de macOS.

## Características

- Conversión de texto a voz en español usando voces nativas de macOS
- Múltiples voces en español disponibles (Monica, Paulina, Juan, Diego)
- Control de velocidad de habla
- Opción para guardar audio en archivos
- Integración completa con Claude Desktop vía MCP

## Requisitos

- macOS (cualquier versión con el comando `say`)
- Node.js v18 o superior
- Claude Desktop instalado

## Instalación

### 1. Clonar o descargar el proyecto

```bash
cd /ruta/donde/quieres/el/proyecto
git clone [URL_DEL_REPO] mcptts
cd mcptts
```

### 2. Instalar dependencias

```bash
npm install
```

### 3. Compilar el proyecto

```bash
npm run build
```

### 4. Configurar Claude Desktop

Edita el archivo de configuración de Claude Desktop:

```bash
code ~/Library/Application\ Support/Claude/claude_desktop_config.json
```

O usando cualquier editor de texto:

```bash
nano ~/Library/Application\ Support/Claude/claude_desktop_config.json
```

Agrega la configuración del servidor MCP:

```json
{
  "mcpServers": {
    "mcptts": {
      "command": "node",
      "args": ["/RUTA_COMPLETA/mcptts/build/index.js"]
    }
  }
}
```

**IMPORTANTE**: Reemplaza `/RUTA_COMPLETA` con la ruta absoluta donde clonaste el proyecto.

Para obtener la ruta completa, ejecuta en el directorio del proyecto:

```bash
pwd
```

### 5. Reiniciar Claude Desktop

Cierra completamente Claude Desktop y vuelve a abrirlo para que cargue el nuevo servidor MCP.

## Uso

Una vez configurado, Claude Desktop tendrá acceso a las siguientes herramientas:

### 1. speak_text

Convierte texto a voz y lo reproduce en voz alta.

**Ejemplo de uso en Claude Desktop:**

```
Por favor usa la herramienta speak_text para leer este texto: "Hola, soy Claude y ahora puedo hablar en español"
```

**Parámetros:**
- `text` (requerido): El texto a leer
- `voice` (opcional): Nombre de la voz (Monica, Paulina, Juan, Diego). Por defecto: Monica
- `rate` (opcional): Velocidad en palabras por minuto (80-500). Por defecto: 200
- `outputFile` (opcional): Ruta para guardar el audio en formato AIFF

**Ejemplos:**

```
# Voz por defecto (Monica)
speak_text con el texto "Buenos días"

# Con voz específica
speak_text con el texto "Hola mundo" usando la voz Juan

# Con velocidad personalizada
speak_text con el texto "Esto es una prueba de velocidad" a 150 palabras por minuto

# Guardar en archivo
speak_text con el texto "Guardar este audio" y guardar en ~/Desktop/audio.aiff
```

### 2. list_voices

Lista todas las voces en español disponibles en tu sistema macOS.

**Ejemplo:**

```
Muéstrame qué voces en español están disponibles
```

### 3. stop_speech

Detiene cualquier reproducción de voz en curso.

**Ejemplo:**

```
Detén la reproducción actual
```

## Voces en Español

Las voces principales disponibles en macOS son:

- **Monica**: Español mexicano (femenina)
- **Paulina**: Español mexicano (femenina)
- **Juan**: Español de España (masculina)
- **Diego**: Español argentino (masculina)

Para ver todas las voces instaladas en tu sistema:

```bash
say -v "?"
```

Para instalar más voces:
1. Ve a Configuración del Sistema > Accesibilidad > Contenido Hablado
2. Haz clic en "Voces del sistema"
3. Descarga las voces en español que desees

## Desarrollo

### Estructura del proyecto

```
mcptts/
├── src/
│   ├── index.ts           # Servidor MCP principal
│   ├── tts-service.ts     # Lógica de TTS
│   └── types.ts           # Definiciones de tipos
├── build/                 # Código compilado (generado)
├── package.json
├── tsconfig.json
└── README.md
```

### Scripts disponibles

```bash
# Compilar el proyecto
npm run build

# Compilar en modo watch (recompila al guardar)
npm run watch
```

### Probar el servidor manualmente

Puedes probar el servidor sin Claude Desktop:

```bash
node build/index.js
```

El servidor se quedará esperando comandos en stdin siguiendo el protocolo MCP.

## Solución de Problemas

### El servidor no aparece en Claude Desktop

1. Verifica que la ruta en `claude_desktop_config.json` sea absoluta y correcta
2. Asegúrate de haber compilado el proyecto con `npm run build`
3. Reinicia Claude Desktop completamente
4. Revisa los logs de Claude Desktop en: `~/Library/Logs/Claude/`

### Error "command not found: say"

El comando `say` solo está disponible en macOS. Este servidor no funcionará en Windows o Linux.

### La voz no suena o hay error de audio

1. Verifica que tu Mac tenga el volumen activado
2. Prueba el comando manualmente: `say -v Monica "prueba"`
3. Verifica que la voz esté instalada en Configuración del Sistema

### Errores de compilación

```bash
# Limpia y reinstala
rm -rf node_modules build
npm install
npm run build
```

## Limitaciones

- Solo funciona en macOS (depende del comando `say`)
- Requiere invocación manual desde Claude Desktop (no hay auto-reproducción)
- El comando `say` es bloqueante durante la reproducción
- Textos muy largos pueden tardar en procesarse

## Mejoras Futuras

- Soporte para dividir textos largos en chunks
- Cola de reproducción
- Más opciones de personalización de voz
- Soporte para SSML
- Indicador de progreso para textos largos

## Licencia

ISC

## Contribuciones

Las contribuciones son bienvenidas. Por favor, abre un issue o pull request.

## Autor

Creado para integrar TTS nativo de macOS con Claude Desktop vía MCP.
