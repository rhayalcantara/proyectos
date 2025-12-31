# Plan: Sonido de Recepción de Mensajes Configurable

## Objetivo
Implementar un sistema de notificación sonora cuando se recibe un mensaje, con opciones configurables para el usuario.

---

## Fase 1: Archivos de Audio

### 1.1 Agregar sonidos predeterminados
- Crear carpeta `public/sounds/`
- Agregar sonidos de notificación:
  - `notification-1.mp3` - Sonido clásico WhatsApp
  - `notification-2.mp3` - Tono suave
  - `notification-3.mp3` - Tono discreto
  - `none` - Sin sonido

---

## Fase 2: Servicio de Configuración

### 2.1 Crear NotificationSettingsService
**Archivo:** `src/app/core/services/notification-settings.service.ts`

```typescript
interface NotificationSettings {
  soundEnabled: boolean;
  soundFile: string;
  volume: number; // 0-100
  playWhenFocused: boolean; // Reproducir incluso con la app enfocada
}
```

**Funcionalidades:**
- `getSettings()` - Obtener configuración actual
- `updateSettings(settings)` - Guardar configuración
- `playNotificationSound()` - Reproducir sonido según configuración
- Persistencia en localStorage

---

## Fase 3: Integración con SignalR

### 3.1 Modificar SignalRService
**Archivo:** `src/app/core/services/signalr.service.ts`

**Cambios:**
- Inyectar `NotificationSettingsService`
- En el evento `ReceiveMessage`:
  - Verificar si el mensaje no es propio
  - Verificar si el chat no está actualmente seleccionado (opcional)
  - Llamar a `playNotificationSound()`

---

## Fase 4: UI de Configuración

### 4.1 Crear componente de configuración
**Archivo:** `src/app/features/settings/notification-settings.component.ts`

**Opciones a mostrar:**
- Toggle: Activar/desactivar sonido
- Selector: Elegir sonido de la lista
- Slider: Volumen (0-100%)
- Toggle: Reproducir cuando la app está enfocada
- Botón: Probar sonido

### 4.2 Integrar en el menú del chat-list
- Agregar botón de configuración (icono engranaje)
- Abrir modal/panel de configuración

---

## Fase 5: Lógica de Reproducción

### 5.1 Condiciones para reproducir sonido
1. Sonido habilitado en configuración
2. El mensaje NO es del usuario actual
3. (Opcional) El chat del mensaje NO está seleccionado
4. (Opcional) La pestaña del navegador NO está enfocada

### 5.2 Implementación con Audio API
```typescript
private audio: HTMLAudioElement;

playNotificationSound(): void {
  const settings = this.getSettings();
  if (!settings.soundEnabled) return;

  this.audio = new Audio(`/sounds/${settings.soundFile}`);
  this.audio.volume = settings.volume / 100;
  this.audio.play().catch(err => console.log('Audio blocked:', err));
}
```

---

## Estructura de Archivos

```
clone-whatsapp-web/
├── public/
│   └── sounds/
│       ├── notification-1.mp3
│       ├── notification-2.mp3
│       └── notification-3.mp3
└── src/app/
    ├── core/services/
    │   └── notification-settings.service.ts (NUEVO)
    └── features/
        └── settings/
            ├── notification-settings.component.ts (NUEVO)
            ├── notification-settings.component.html (NUEVO)
            └── notification-settings.component.scss (NUEVO)
```

---

## Tareas

| # | Tarea | Estado |
|---|-------|--------|
| 1 | Obtener/crear archivos de audio MP3 | Pendiente |
| 2 | Crear NotificationSettingsService | Pendiente |
| 3 | Integrar sonido en SignalRService | Pendiente |
| 4 | Crear componente de configuración | Pendiente |
| 5 | Agregar botón de configuración en chat-list | Pendiente |
| 6 | Probar en diferentes navegadores | Pendiente |

---

## Consideraciones Técnicas

1. **Autoplay Policy**: Los navegadores bloquean audio automático. El sonido solo funcionará después de que el usuario interactúe con la página.

2. **Formato de audio**: MP3 es compatible con todos los navegadores modernos.

3. **Tamaño de archivos**: Mantener los MP3 pequeños (<50KB cada uno) para carga rápida.

4. **localStorage**: Guardar configuración localmente para persistencia sin backend.

---

## Tiempo Estimado
- Fase 1: 10 min (obtener audios)
- Fase 2: 20 min (servicio)
- Fase 3: 15 min (integración SignalR)
- Fase 4: 30 min (UI)
- Fase 5: 15 min (lógica y pruebas)

**Total: ~1.5 horas**
