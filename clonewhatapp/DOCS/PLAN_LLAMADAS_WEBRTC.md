# Plan de Implementación - Llamadas WebRTC

**Fecha**: 2025-12-30
**Fase**: 5.2 - Llamadas de Voz y Video

---

## Resumen

Implementar llamadas de voz y video en tiempo real usando WebRTC con SignalR como canal de señalización.

---

## Arquitectura WebRTC

```
Usuario A                    Servidor                    Usuario B
    |                           |                            |
    |-- Iniciar llamada ------->|                            |
    |                           |-- Notificar llamada ------>|
    |                           |                            |
    |                           |<-- Aceptar/Rechazar -------|
    |<-- Respuesta -------------|                            |
    |                           |                            |
    |============ Intercambio SDP/ICE via SignalR ===========|
    |                           |                            |
    |<================ Conexión P2P directa ================>|
    |                           |                            |
```

### Flujo de señalización:
1. Usuario A inicia llamada → SignalR notifica a Usuario B
2. Usuario B acepta → SignalR notifica a Usuario A
3. Intercambio de SDP Offer/Answer via SignalR
4. Intercambio de ICE Candidates via SignalR
5. Conexión P2P establecida → Audio/Video directo

---

## Plan de Tareas

### Backend (4 tareas)

| # | Tarea | Descripción |
|---|-------|-------------|
| B1 | Crear modelo Llamada | Entidad para historial de llamadas |
| B2 | Eventos SignalR para llamadas | CallOffer, CallAnswer, CallReject, CallEnd, ICECandidate |
| B3 | Endpoint historial llamadas | GET /api/calls - Lista de llamadas recientes |
| B4 | Endpoint estado llamada | POST /api/calls/start, POST /api/calls/end |

### Frontend (8 tareas)

| # | Tarea | Descripción |
|---|-------|-------------|
| F1 | WebRTCService | Servicio para manejar RTCPeerConnection |
| F2 | CallService | Servicio para estado y lógica de llamadas |
| F3 | Botones llamar en header | Iconos de teléfono y video en chat-header |
| F4 | IncomingCallComponent | Modal de llamada entrante |
| F5 | ActiveCallComponent | Pantalla durante llamada |
| F6 | Controles de llamada | Mute, video on/off, speaker, colgar |
| F7 | Integrar eventos SignalR | Escuchar y emitir eventos de llamada |
| F8 | Manejo de permisos | Solicitar cámara/micrófono |

---

## Detalle Técnico

### B1: Modelo Llamada

```csharp
public enum TipoLlamada { Voz, Video }
public enum EstadoLlamada { Perdida, Contestada, Rechazada }

public class Llamada
{
    public Guid Id { get; set; }
    public Guid LlamadorId { get; set; }
    public Guid ReceptorId { get; set; }
    public TipoLlamada Tipo { get; set; }
    public EstadoLlamada Estado { get; set; }
    public DateTime FechaInicio { get; set; }
    public DateTime? FechaFin { get; set; }
    public int? DuracionSegundos { get; set; }
}
```

### B2: Eventos SignalR

```csharp
// En ChatHub.cs
Task CallUser(string targetUserId, string callType, string sdpOffer);
Task AnswerCall(string callerId, string sdpAnswer);
Task RejectCall(string callerId);
Task EndCall(string otherUserId);
Task SendICECandidate(string targetUserId, string candidate);

// Eventos a clientes
ReceiveCallOffer(callerId, callerName, callType, sdpOffer)
ReceiveCallAnswer(sdpAnswer)
CallRejected(reason)
CallEnded()
ReceiveICECandidate(candidate)
```

### F1: WebRTCService

```typescript
@Injectable({ providedIn: 'root' })
export class WebRTCService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;
  private remoteStream = signal<MediaStream | null>(null);

  // ICE Servers (STUN gratuitos)
  private iceServers = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ];

  async createOffer(isVideo: boolean): Promise<string>;
  async createAnswer(sdpOffer: string): Promise<string>;
  addICECandidate(candidate: string): void;
  endCall(): void;
}
```

### F4: IncomingCallComponent

- Foto/nombre del llamador
- Tipo de llamada (voz/video)
- Botón Aceptar (verde)
- Botón Rechazar (rojo)
- Sonido de ring

### F5: ActiveCallComponent

- Video remoto (pantalla completa si video)
- Video local (esquina pequeña)
- Duración de llamada
- Controles: mute, video, speaker, colgar

---

## Configuración ICE/STUN

Para producción se necesitaría un servidor TURN, pero para desarrollo local usaremos STUN público:

```typescript
const iceServers = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  { urls: 'stun:stun2.l.google.com:19302' }
];
```

---

## Orden de Implementación

1. **B1**: Modelo Llamada + migración
2. **B2**: Eventos SignalR para llamadas
3. **F1**: WebRTCService básico
4. **F2**: CallService
5. **F3**: Botones en header
6. **F4**: IncomingCallComponent
7. **F5**: ActiveCallComponent
8. **F6**: Controles de llamada
9. **F7**: Integración SignalR
10. **F8**: Permisos de cámara/mic
11. **B3, B4**: Endpoints de historial (opcional)

---

## Consideraciones

1. **HTTPS requerido**: WebRTC requiere HTTPS en producción (localhost es excepción)
2. **Firewall**: Algunas redes corporativas bloquean WebRTC
3. **TURN Server**: Para producción real, necesario para NAT traversal
4. **Calidad**: Ajustar constraints de video según ancho de banda

---

## Criterios de Aceptación

- [ ] Usuario puede iniciar llamada de voz desde chat
- [ ] Usuario puede iniciar videollamada desde chat
- [ ] Llamada entrante muestra notificación con accept/reject
- [ ] Durante llamada: mute, video toggle, colgar funcionan
- [ ] Audio/video se transmite correctamente
- [ ] Al colgar, ambos usuarios vuelven al chat

---

**Próximo paso**: Aprobar plan e implementar
