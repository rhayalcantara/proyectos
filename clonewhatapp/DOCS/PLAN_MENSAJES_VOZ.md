# Plan de Implementación - Mensajes de Voz

**Fecha**: 2025-12-30
**Fase**: 5.1 - Mensajes de Voz

---

## Resumen

Implementar la funcionalidad de grabación, envío y reproducción de mensajes de voz estilo WhatsApp.

---

## Análisis Previo

### Ya existe:
- Modelo `Mensaje` con `TipoMensaje.Audio`
- Endpoint para envío de archivos (`POST /api/messages/with-file`)
- Almacenamiento de archivos en `/uploads`
- Campos: `UrlArchivo`, `NombreArchivo`, `TamanoArchivo`

### Se necesita agregar:
- Campo `Duracion` en modelo Mensaje (para audio)
- Grabación de audio en frontend (MediaRecorder API)
- Reproductor de audio personalizado
- Visualización de forma de onda (opcional)

---

## Arquitectura

```
Usuario presiona botón grabar
         ↓
MediaRecorder captura audio (webm/opus)
         ↓
Al soltar, se detiene grabación
         ↓
Se envía archivo via POST /api/messages/with-file
         ↓
Backend almacena en /uploads/audio/
         ↓
SignalR notifica a destinatarios
         ↓
Frontend muestra reproductor de audio
```

---

## Plan de Tareas

### Backend (3 tareas)

| # | Tarea | Descripción | Prioridad |
|---|-------|-------------|-----------|
| B1 | Agregar campo Duracion | Migración para agregar `DuracionSegundos` a Mensaje | Alta |
| B2 | Actualizar MessagesController | Procesar duración del audio en endpoint with-file | Alta |
| B3 | Crear carpeta audio | Crear directorio `/uploads/audio` para almacenar audios | Alta |

### Frontend (6 tareas)

| # | Tarea | Descripción | Prioridad |
|---|-------|-------------|-----------|
| F1 | Servicio AudioRecorder | Servicio para manejar MediaRecorder API | Alta |
| F2 | Botón grabar/detener | Botón que cambia entre grabar y enviar texto | Alta |
| F3 | UI durante grabación | Mostrar duración, onda animada, cancelar | Alta |
| F4 | Envío de audio | Integrar con ChatService.sendMessageWithFile | Alta |
| F5 | Componente AudioPlayer | Reproductor personalizado con play/pause, progreso | Alta |
| F6 | Forma de onda (waveform) | Visualización de la forma de onda del audio | Media |

---

## Detalle Técnico

### B1: Campo Duracion

```csharp
// En Mensaje.cs
public int? DuracionSegundos { get; set; }
```

Crear migración EF Core para agregar el campo.

### F1: Servicio AudioRecorder

```typescript
// audio-recorder.service.ts
- startRecording(): Promise<void>
- stopRecording(): Promise<Blob>
- cancelRecording(): void
- isRecording: Signal<boolean>
- recordingDuration: Signal<number>
```

Usará `navigator.mediaDevices.getUserMedia` y `MediaRecorder`.

### F2-F3: UI de Grabación

En `chat-window.component`:
- Botón micrófono a la derecha del input
- Al presionar: inicia grabación
- Durante grabación: muestra duración y botón cancelar/enviar
- Animación de onda o pulso rojo

### F5: AudioPlayer

```typescript
// audio-player.component.ts
@Input() audioUrl: string
@Input() duration: number

- play/pause toggle
- barra de progreso clickeable
- duración actual / total
- velocidad 1x, 1.5x, 2x (opcional)
```

### F6: Forma de Onda

Opciones:
1. **Web Audio API**: Analizar audio y dibujar canvas
2. **Librería**: wavesurfer.js (pesada) o peaks.js
3. **Simplificado**: Barras estáticas aleatorias (como WhatsApp web)

Recomendación: Opción 3 para MVP, después mejorar.

---

## Dependencias Externas

Ninguna nueva. Se usa:
- MediaRecorder API (nativo del navegador)
- Web Audio API (para duración/forma de onda)

---

## Estimación de Complejidad

| Área | Complejidad |
|------|-------------|
| Backend | Baja |
| Frontend - Grabación | Media |
| Frontend - Reproductor | Media |
| Frontend - Waveform | Media-Alta |

---

## Orden de Implementación

1. **B1, B2, B3**: Backend primero (preparar modelo y endpoint)
2. **F1**: Servicio de grabación
3. **F2, F3**: UI de grabación en chat-window
4. **F4**: Integración envío
5. **F5**: Reproductor de audio
6. **F6**: Forma de onda (puede dejarse para después)

---

## Riesgos y Consideraciones

1. **Permisos de micrófono**: Manejar caso donde usuario deniega permiso
2. **Formato de audio**: webm/opus es moderno, pero puede no funcionar en Safari
3. **Tamaño de archivos**: Limitar duración máxima (ej: 5 minutos)
4. **Mobile**: Probar en navegadores móviles

---

## Criterios de Aceptación

- [ ] Usuario puede grabar audio manteniendo presionado el botón
- [ ] Se muestra duración durante grabación
- [ ] Usuario puede cancelar grabación
- [ ] Audio se envía y almacena correctamente
- [ ] Destinatario recibe notificación en tiempo real
- [ ] Reproductor permite play/pause
- [ ] Barra de progreso funciona
- [ ] Se muestra duración del audio

---

**Próximo paso**: Discutir este plan y proceder con la implementación.
