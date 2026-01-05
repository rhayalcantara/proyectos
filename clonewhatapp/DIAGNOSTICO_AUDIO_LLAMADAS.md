# Diagnóstico: Audio no se transmite en llamadas

## Fecha: 2026-01-01

## Problema Reportado
La llamada se realiza (la conexión se establece) pero el audio no llega al otro lado.

---

## Problemas Identificados

### 1. Inconsistencia de Puertos (CRÍTICO)

**Backend configurado en:** `http://localhost:5181` (launchSettings.json)
**Frontend apunta a:** `http://localhost:5139/api` (environment.ts)

Esta inconsistencia significa que el frontend no se está comunicando correctamente con el backend.

**Solución:**
```typescript
// clone-whatsapp-web/src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:5181/api',      // Cambiar 5139 a 5181
  hubUrl: 'http://localhost:5181/hubs/chat'  // Cambiar 5139 a 5181
};
```

### 2. ICE Candidates para Audio (posible)

El mismo problema que se resolvió para video (commit 9c9e25a) podría estar afectando audio:
- Los ICE candidates llegan antes de que el `peerConnection` esté listo
- Se encolan pero podrían no procesarse en el momento correcto

**Diagnóstico necesario:** Revisar console.log en el navegador para ver:
- ¿Se reciben ICE candidates?
- ¿Se procesan los candidates pendientes?
- ¿Qué dice el connection state?

### 3. STUN Servers vs Red Local

Los STUN servers de Google solo funcionan para NAT traversal en internet.
Para pruebas entre dispositivos en la **misma red local**, WebRTC debería funcionar
directamente, pero puede haber problemas si:
- El firewall bloquea UDP
- Los puertos WebRTC están cerrados

---

## Verificaciones en el Navegador

Abrir la consola del desarrollador (F12) y buscar estos logs:

```
✓ "Connection state: connected"         → Conexión WebRTC establecida
✓ "Remote stream recibido: X tracks"    → Se recibió stream remoto
✓ "Procesando X ICE candidates..."      → Se procesaron candidates pendientes
```

**Si ves "Connection state: failed" o "disconnected"** → Problema de ICE/NAT

---

## Pasos para Probar en Red Local

### Paso 1: Obtener IP Local
```bash
# En Mac:
ifconfig | grep "inet " | grep -v 127.0.0.1
# Ejemplo: 192.168.1.100
```

### Paso 2: Configurar Backend para escuchar en todas las interfaces

Modificar `CloneWhatsApp.API/Properties/launchSettings.json`:
```json
{
  "profiles": {
    "http": {
      "commandName": "Project",
      "dotnetRunMessages": true,
      "launchBrowser": false,
      "applicationUrl": "http://0.0.0.0:5181",
      "environmentVariables": {
        "ASPNETCORE_ENVIRONMENT": "Development"
      }
    }
  }
}
```

O ejecutar con Kestrel directamente:
```bash
cd CloneWhatsApp.API
dotnet run --urls "http://0.0.0.0:5181"
```

### Paso 3: Configurar Frontend para red local

Crear archivo `clone-whatsapp-web/src/environments/environment.local.ts`:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://192.168.X.X:5181/api',      // Tu IP local
  hubUrl: 'http://192.168.X.X:5181/hubs/chat'
};
```

Modificar `angular.json` para usar este environment o cambiar temporalmente `environment.ts`.

### Paso 4: Actualizar CORS en Backend

En `Program.cs`, agregar la IP local al CORS:
```csharp
policy.WithOrigins(
    "http://localhost:4200",
    "http://192.168.X.X:4200",  // Tu IP local
    // ... otros origins
)
```

### Paso 5: Ejecutar Angular para acceso en red
```bash
cd clone-whatsapp-web
ng serve --host 0.0.0.0 --disable-host-check
```

### Paso 6: Acceder desde otro dispositivo
- Dispositivo 1: `http://192.168.X.X:4200`
- Dispositivo 2: `http://192.168.X.X:4200`

---

## Verificación del Audio Stream

El código actual en `webrtc.service.ts:46-57` solicita audio correctamente:
```typescript
this.localStream = await navigator.mediaDevices.getUserMedia({
  audio: true,  // ← Siempre pide audio
  video: isVideo ? { ... } : false
});
```

Para verificar que el audio se está capturando:
1. Abrir DevTools → Application → Permissions
2. Verificar que microphone está "allowed"

---

## Logs de Diagnóstico Adicionales

Agregar estos logs temporalmente en `webrtc.service.ts`:

```typescript
// En initializeConnection, después de crear peerConnection
console.log('PeerConnection created, ICE gathering state:', this.peerConnection.iceGatheringState);

// En ontrack
this.peerConnection.ontrack = (event) => {
  console.log('ontrack event:', {
    trackKind: event.track.kind,  // "audio" o "video"
    trackEnabled: event.track.enabled,
    trackMuted: event.track.muted,
    streams: event.streams.length
  });
  this.remoteStream.set(event.streams[0]);
};

// En addIceCandidate
console.log('Adding ICE candidate:', candidateStr.substring(0, 100) + '...');
```

---

## Posible Solución: Servidor TURN

Si las pruebas en red local fallan, considera agregar un servidor TURN:

```typescript
private readonly iceServers: RTCIceServer[] = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  // Agregar TURN server (puedes usar uno gratuito para testing)
  {
    urls: 'turn:numb.viagenie.ca',
    username: 'webrtc@live.com',
    credential: 'muazkh'
  }
];
```

O usar un servicio como Twilio TURN o configurar tu propio coturn server.

---

## Resumen de Cambios Necesarios

| Archivo | Cambio |
|---------|--------|
| `environment.ts` | Cambiar puerto 5139 → 5181 |
| `launchSettings.json` | Cambiar localhost → 0.0.0.0 (para red) |
| `Program.cs` | Agregar IP local al CORS |
| `angular.json` | (opcional) Configurar environment local |

---

## Próximos Pasos

1. [ ] Corregir la inconsistencia de puertos (5139 vs 5181)
2. [ ] Ejecutar la app y verificar logs en consola
3. [ ] Probar llamada entre dos pestañas del mismo navegador
4. [ ] Si funciona localmente, configurar para red local
5. [ ] Probar entre dos dispositivos
