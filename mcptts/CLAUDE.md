# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an MCP (Model Context Protocol) server that exposes Text-to-Speech capabilities to Claude Desktop using macOS's native `say` command. The server communicates with Claude Desktop via stdio using JSON-RPC following the MCP protocol.

## Architecture

```
Claude Desktop (MCP Client)
    ↓ stdio (JSON-RPC)
MCP Server (src/index.ts)
    ↓ calls
TTS Service (src/tts-service.ts)
    ↓ executes
macOS `say` command
```

### Key Components

- **src/index.ts**: MCP server implementation with two main handlers:
  - `ListToolsRequestSchema`: Lists available tools
  - `CallToolRequestSchema`: Executes tool calls

- **src/tts-service.ts**: Core TTS logic that wraps macOS `say` command with:
  - Input sanitization (escapes quotes and backslashes)
  - Parameter validation (rate clamped to 80-500 WPM)
  - Error handling with structured responses

- **src/types.ts**: TypeScript interfaces for `Voice`, `SpeakOptions`, and `TTSResult`

### Tools Exposed via MCP

1. **speak_text**: Main TTS function
   - Required: `text` (string)
   - Optional: `voice` (default: "Monica"), `rate` (default: 200 WPM), `outputFile` (AIFF format)

2. **list_voices**: Returns Spanish voices available on the system
   - Falls back to predefined list (Monica, Paulina, Juan, Diego) if system query fails

3. **stop_speech**: Terminates any ongoing `say` process via `killall say`

## Development Commands

### Build
```bash
npm run build
```
Compiles TypeScript to `build/` directory and makes `build/index.js` executable.

### Watch Mode
```bash
npm run watch
```
Recompiles automatically on file changes (useful during active development).

### Run Server Manually
```bash
node build/index.js
```
Starts the MCP server. It will wait for JSON-RPC commands via stdin.

## TypeScript Configuration

- **Target**: ES2022 with Node16 modules (ESM)
- **Strict mode**: Enabled
- **Output**: Generates `.d.ts` declaration files and source maps
- **Module resolution**: Node16 (supports native ESM imports with `.js` extensions)

## Security Considerations

### Input Sanitization
The `sanitizeText()` function in `tts-service.ts` prevents command injection:
```typescript
text.replace(/\\/g, '\\\\').replace(/"/g, '\\"')
```

**Critical**: Always sanitize user input before passing to `exec()` to prevent arbitrary command execution.

### Known Risk
The `outputFile` parameter is sanitized but not path-validated. Malicious paths could overwrite arbitrary files. Consider adding path validation if exposing this parameter to untrusted input.

### Timeout Protection
All `exec()` calls use 60-second timeout to prevent indefinite hangs.

## Integration with Claude Desktop

The server is configured in `~/Library/Application Support/Claude/claude_desktop_config.json`:
```json
{
  "mcpServers": {
    "mcptts": {
      "command": "node",
      "args": ["/absolute/path/to/mcptts/build/index.js"]
    }
  }
}
```

**Important**: The path must be absolute. Claude Desktop spawns this as a child process and maintains stdio communication throughout the session.

## Design Patterns

### Modular Separation
- **index.ts**: Protocol layer (MCP communication)
- **tts-service.ts**: Business logic (TTS operations)
- **types.ts**: Type contracts

This separation facilitates unit testing and makes the TTS service reusable outside of MCP context.

### Typed Result Pattern
Functions return structured objects instead of throwing exceptions:
```typescript
{ success: boolean, message: string, error?: string }
```
This makes error handling predictable and explicit.

### Graceful Fallback
If `say -v "?"` fails to list system voices, the service falls back to a predefined list of Spanish voices, ensuring the service always remains functional.

## Platform Limitations

- **macOS only**: Depends on the `say` command which is macOS-specific
- **Blocking execution**: The `say` command blocks until audio completes
- **Manual invocation**: Users must explicitly call tools; there's no auto-capture of Claude's responses
- **AIFF only**: Output files are limited to AIFF format

## Future Improvement Opportunities

- Add chunking for very long texts
- Implement the `volume` parameter (currently defined but unused in types.ts:13)
- Add path validation for `outputFile` parameter
- Support additional audio formats (MP3, WAV)
- Add SSML support for fine-grained prosody control
