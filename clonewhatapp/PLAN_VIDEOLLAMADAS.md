# Plan de Solución: Video no visible para el receptor en videollamadas

## Estado: COMPLETADO

---

## Descripción del Problema

### Síntomas Reportados
- **El que LLAMA (Caller):** Ve correctamente AMBOS videos (su propio video y el del receptor) ✅
- **El que RECIBE (Callee):** Solo ve SU PROPIO video, pero NO ve el video del caller ❌
- **Nota importante:** El callee SÍ envía su video correctamente (porque el caller lo puede ver)

### Causa Raíz Identificada

**Problema 1: ICE Candidates perdidos**
Los ICE candidates del caller llegaban al callee ANTES de que este inicializara su `peerConnection` (lo cual ocurre solo cuando el usuario acepta la llamada). Los candidates se descartaban silenciosamente en `webrtc.service.ts:108`.

**Problema 2: Video remoto no asignado**
El componente `ActiveCallComponent` usaba un `effect()` para asignar el stream remoto, pero el stream podía estar disponible ANTES de que el componente se montara, causando que el video nunca se asignara.

---

## Solución Implementada

### Tarea 1: Cola de ICE Candidates Pendientes ✅
**Archivo:** `webrtc.service.ts`

```typescript
// Nuevo array para almacenar candidates pendientes
private pendingIceCandidates: string[] = [];

// Modificación en addIceCandidate()
async addIceCandidate(candidateStr: string): Promise<void> {
  if (!this.peerConnection) {
    console.log('ICE candidate recibido antes de peerConnection, encolando...');
    this.pendingIceCandidates.push(candidateStr);
    return;
  }
  // ... procesar normalmente
}

// Nuevo método para procesar cola
private async processPendingIceCandidates(): Promise<void> {
  for (const candidateStr of this.pendingIceCandidates) {
    await this.peerConnection?.addIceCandidate(new RTCIceCandidate(JSON.parse(candidateStr)));
  }
  this.pendingIceCandidates = [];
}

// Llamado en createAnswer() después de setRemoteDescription
await this.processPendingIceCandidates();
```

### Tarea 2: Limpieza de Cola en endCall() ✅
**Archivo:** `webrtc.service.ts`

```typescript
endCall(): void {
  // ... código existente ...
  this.pendingIceCandidates = [];
}
```

### Tarea 3: Logs de Diagnóstico ✅
**Archivo:** `webrtc.service.ts`

- Log cuando se encolan ICE candidates
- Log cuando se procesan candidates pendientes
- Log cuando se recibe remote stream

### Tarea 4: Fix de Video Remoto en UI ✅
**Archivo:** `active-call.component.ts`

```typescript
ngAfterViewInit(): void {
  // Configurar video local
  const localStream = this.webrtcService.getLocalStream2();
  if (localStream && this.localVideoRef?.nativeElement) {
    this.localVideoRef.nativeElement.srcObject = localStream;
  }

  // Configurar video remoto si ya está disponible
  const remoteStream = this.webrtcService.remoteStream();
  if (remoteStream && this.remoteVideoRef?.nativeElement) {
    this.remoteVideoRef.nativeElement.srcObject = remoteStream;
  }
}
```

### Fix Adicional: URL de Foto de Perfil ✅
**Archivo:** `incoming-call.component.ts`

```typescript
get callerPhoto(): string | undefined {
  const photo = this.callService.callState().callerPhoto;
  if (!photo) return undefined;
  if (photo.startsWith('http')) return photo;
  return `${this.apiBaseUrl}${photo}`;
}
```

---

## Archivos Modificados

| Archivo | Cambios |
|---------|---------|
| `webrtc.service.ts` | Cola de ICE candidates, procesamiento pendiente, logs |
| `active-call.component.ts` | Asignación de video remoto en ngAfterViewInit |
| `incoming-call.component.ts` | URL completa para foto de perfil |

---

## Resultado

**ÉXITO:** Ambos usuarios (caller y callee) ahora pueden verse mutuamente en videollamadas.

### Logs esperados en consola del receptor:
```
ICE candidate recibido antes de peerConnection, encolando...
Procesando 16 ICE candidates pendientes...
Remote stream recibido: 2 tracks
Connection state: connected
ngAfterViewInit: Asignando remote stream al video
```
