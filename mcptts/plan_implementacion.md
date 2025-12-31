# Plan de Implementación: MCP TTS para Claude Desktop en Mac

## 1. ANÁLISIS DEL PROBLEMA

### Situación Actual
- Claude Desktop no tiene capacidad nativa de Text-to-Speech
- Las respuestas de Claude son solo texto
- No existe un hook automático para reproducir respuestas

### Objetivo
Crear un MCP server que permita convertir texto a voz usando el TTS nativo de macOS, integrándose con Claude Desktop.

---

## 2. ARQUITECTURA DE LA SOLUCIÓN

### Componentes Principales

1. **MCP Server (Node.js/TypeScript)**
   - Servidor que implementa el protocolo MCP
   - Expone herramientas de TTS a Claude Desktop
   - Utiliza el comando `say` de macOS

2. **Herramientas Expuestas**
   - `speak_text`: Reproduce texto en voz alta
   - `list_voices`: Lista voces disponibles en español
   - `stop_speech`: Detiene reproducción actual (opcional)

3. **Configuración en Claude Desktop**
   - Registro del MCP server en `claude_desktop_config.json`
   - Configuración de ruta y parámetros

---

## 3. ESPECIFICACIÓN TÉCNICA

### Tecnologías
- **Runtime**: Node.js (v18+)
- **Lenguaje**: TypeScript
- **SDK**: @modelcontextprotocol/sdk
- **TTS Engine**: macOS `say` command

### Voces en Español Recomendadas
- **Monica**: Español mexicano (femenina)
- **Paulina**: Español mexicano (femenina)
- **Juan**: Español de España (masculina)
- **Diego**: Español argentino (masculina)

### Parámetros Configurables
- Velocidad de habla (words per minute)
- Voz seleccionada
- Volumen
- Archivo de salida (opcional, para guardar audio)

---

## 4. ESTRUCTURA DEL PROYECTO

```
mcptts/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts          # Servidor MCP principal
│   ├── tts-service.ts    # Lógica de TTS
│   └── types.ts          # Definiciones de tipos
├── build/                # Código compilado
├── README.md
└── plan_implementacion.md
```

---

## 5. FUNCIONALIDADES PRINCIPALES

### Fase 1: Implementación Básica
- [x] Servidor MCP con comunicación stdio
- [x] Herramienta `speak_text` básica
- [x] Manejo de errores
- [x] Configuración en Claude Desktop

### Fase 2: Mejoras
- [ ] Lista de voces disponibles (`list_voices`)
- [ ] Control de velocidad de habla
- [ ] Control de volumen
- [ ] Detener reproducción en curso

### Fase 3: Funcionalidades Avanzadas
- [ ] Guardar audio en archivo
- [ ] Reproducción asíncrona con callback
- [ ] Soporte para SSML (Speech Synthesis Markup Language)
- [ ] Cola de reproducción para múltiples textos

---

## 6. LIMITACIONES Y CONSIDERACIONES

### Limitaciones Técnicas
1. **Invocación Manual**: El usuario debe llamar explícitamente la herramienta
2. **Solo macOS**: Depende del comando `say` nativo
3. **Sin Auto-reproducción**: No hay hook para capturar automáticamente respuestas

### Seguridad
- Sanitizar entrada de texto para evitar inyección de comandos
- Escape de comillas y caracteres especiales
- Validación de parámetros de voz

### Performance
- El comando `say` es bloqueante
- Considerar límite de longitud de texto
- Posible división de textos largos en chunks

---

## 7. ALTERNATIVAS CONSIDERADAS

### Opción A: MCP Server con `say` (RECOMENDADA)
**Pros:**
- Integración nativa con macOS
- No requiere dependencias externas
- Voces de alta calidad
- Soporte nativo para español

**Contras:**
- Solo funciona en macOS
- Invocación manual requerida

### Opción B: Script de Monitoreo de Portapapeles
**Pros:**
- Reproducción automática al copiar
- No requiere invocación manual

**Contras:**
- Menos integrado con Claude Desktop
- Requiere proceso en background
- Dependencia de acción del usuario (copiar)

### Opción C: MCP Server con API Externa (ElevenLabs, Google TTS)
**Pros:**
- Multiplataforma
- Voces más naturales (ElevenLabs)

**Contras:**
- Requiere conexión a internet
- Costos asociados
- Latencia de red
- Complejidad de configuración

---

## 8. PLAN DE IMPLEMENTACIÓN

### Tareas de Desarrollo

#### **FASE 1: Setup del Proyecto**
1. Inicializar proyecto Node.js con TypeScript
2. Instalar dependencias del SDK de MCP
3. Configurar tsconfig.json
4. Crear estructura de carpetas

#### **FASE 2: Implementación del MCP Server**
5. Implementar servidor MCP básico
6. Crear servicio de TTS usando `say`
7. Implementar herramienta `speak_text`
8. Añadir manejo de errores y validación
9. Compilar y probar localmente

#### **FASE 3: Configuración de Claude Desktop**
10. Crear archivo de configuración JSON
11. Registrar MCP server en Claude Desktop
12. Probar integración end-to-end

#### **FASE 4: Mejoras y Documentación**
13. Implementar `list_voices`
14. Añadir control de velocidad y volumen
15. Escribir README con instrucciones
16. Crear ejemplos de uso

#### **FASE 5: Funcionalidades Avanzadas (Opcional)**
17. Implementar `stop_speech`
18. Añadir soporte para guardar audio
19. Implementar cola de reproducción
20. Optimizar para textos largos

---

## 9. CRITERIOS DE ÉXITO

- ✓ Claude Desktop puede invocar el MCP server
- ✓ El texto se reproduce correctamente en español
- ✓ Las voces disponibles son accesibles
- ✓ No hay errores de inyección de comandos
- ✓ El rendimiento es aceptable (<2s para textos cortos)
- ✓ La documentación es clara y completa

---

## 10. PRÓXIMOS PASOS

1. **Discutir este plan**: Revisar y ajustar según necesidades
2. **Aprobar arquitectura**: Confirmar enfoque técnico
3. **Iniciar implementación**: Comenzar con Fase 1
4. **Iteración**: Implementar, probar, mejorar

---

## 11. PREGUNTAS PARA DISCUSIÓN

1. ¿Prefieres comenzar con la implementación básica o incluir todas las funcionalidades desde el inicio?
2. ¿Necesitas soporte para guardar audio en archivos o solo reproducción en tiempo real?
3. ¿Qué voz en español prefieres como predeterminada?
4. ¿Considerar implementar la opción de monitoreo de portapapeles como alternativa?
5. ¿Hay algún caso de uso específico que debamos considerar?

---

**Fecha de Creación**: 2025-11-12
**Fecha de Finalización**: 2025-11-12
**Versión**: 1.0
**Estado**: ✅ COMPLETADO

---

## 12. RESUMEN DE IMPLEMENTACIÓN

### Tareas Completadas

✅ **FASE 1: Setup del Proyecto**
- Proyecto Node.js inicializado con TypeScript
- Dependencias del SDK MCP instaladas
- tsconfig.json configurado correctamente
- Estructura de carpetas creada

✅ **FASE 2: Implementación del MCP Server**
- Servidor MCP básico implementado en src/index.ts
- Servicio de TTS creado en src/tts-service.ts
- Herramienta speak_text implementada con todas sus opciones
- Manejo de errores y validación completos
- Sanitización de entrada para prevenir inyección de comandos
- Compilación exitosa

✅ **FASE 3: Herramientas Adicionales**
- Herramienta list_voices implementada
- Herramienta stop_speech implementada
- Control de velocidad de habla (rate)
- Soporte para guardar audio en archivos

✅ **FASE 4: Documentación**
- README.md completo con instrucciones detalladas
- Archivo de ejemplo de configuración creado
- Documentación de uso y troubleshooting
- .gitignore configurado

### Archivos Creados

```
mcptts/
├── src/
│   ├── index.ts           ✅ Servidor MCP principal
│   ├── tts-service.ts     ✅ Lógica de TTS
│   └── types.ts           ✅ Definiciones de tipos
├── build/                 ✅ Código compilado
├── package.json           ✅ Configuración del proyecto
├── tsconfig.json          ✅ Configuración de TypeScript
├── README.md              ✅ Documentación completa
├── .gitignore             ✅ Archivos a ignorar
├── claude_desktop_config.example.json  ✅ Ejemplo de configuración
└── plan_implementacion.md ✅ Este archivo
```

### Funcionalidades Implementadas

1. **speak_text**: Convierte texto a voz con opciones de:
   - Selección de voz (Monica, Paulina, Juan, Diego)
   - Control de velocidad (80-500 palabras por minuto)
   - Guardado de audio en archivos AIFF
   - Sanitización de entrada para seguridad

2. **list_voices**: Lista todas las voces en español disponibles en el sistema

3. **stop_speech**: Detiene cualquier reproducción en curso

### Próximos Pasos para el Usuario

1. Editar `claude_desktop_config.json` con la ruta correcta del proyecto
2. Reiniciar Claude Desktop
3. Probar las herramientas desde Claude Desktop
4. ¡Disfrutar de las capacidades de TTS en español!

### Posibles Mejoras Futuras

- División automática de textos muy largos
- Cola de reproducción para múltiples mensajes
- Soporte para SSML (Speech Synthesis Markup Language)
- Indicador visual de progreso de reproducción
- Configuración de voz predeterminada personalizable
