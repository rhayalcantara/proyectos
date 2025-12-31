import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WebRTCService } from './webrtc.service';
import { SignalRService } from './signalr.service';

export interface CallState {
  isInCall: boolean;
  isIncoming: boolean;
  isVideo: boolean;
  callerId?: string;
  callerName?: string;
  callerPhoto?: string;
  targetUserId?: string;
  targetUserName?: string;
  callId?: string;
  startTime?: Date;
}

export interface CallHistoryItem {
  id: string;
  esLlamadaSaliente: boolean;
  otroUsuarioId: string;
  otroUsuarioNombre: string;
  otroUsuarioFoto?: string;
  tipo: 'Voz' | 'Video';
  estado: 'EnCurso' | 'Completada' | 'Perdida' | 'Rechazada';
  fechaInicio: Date;
  duracionSegundos?: number;
}

@Injectable({
  providedIn: 'root'
})
export class CallService {
  private readonly apiUrl = environment.apiUrl;

  private _callState = signal<CallState>({
    isInCall: false,
    isIncoming: false,
    isVideo: false
  });

  callState = this._callState.asReadonly();
  callDuration = signal<number>(0);
  private durationInterval: number | null = null;

  isInCall = computed(() => this._callState().isInCall);
  isIncoming = computed(() => this._callState().isIncoming);
  isVideo = computed(() => this._callState().isVideo);

  constructor(
    private http: HttpClient,
    private webrtcService: WebRTCService,
    private signalrService: SignalRService
  ) {
    this.setupSignalRListeners();
  }

  private setupSignalRListeners(): void {
    // Llamada entrante
    this.signalrService.onCallOffer((data) => {
      this._callState.set({
        isInCall: true,
        isIncoming: true,
        isVideo: data.callType === 'Video',
        callerId: data.callerId,
        callerName: data.callerName,
        callerPhoto: data.callerPhoto
      });

      // Guardar SDP offer para usarla al aceptar
      (window as any).__pendingSdpOffer = data.sdpOffer;
    });

    // Respuesta a llamada
    this.signalrService.onCallAnswer(async (data) => {
      await this.webrtcService.handleAnswer(data.sdpAnswer);
      this.startDurationTimer();
    });

    // Llamada rechazada
    this.signalrService.onCallRejected((data) => {
      this.endCallLocally();
      alert(`Llamada rechazada: ${data.reason}`);
    });

    // Llamada terminada
    this.signalrService.onCallEnded(() => {
      this.endCallLocally();
    });

    // ICE candidates
    this.signalrService.onIceCandidate((data) => {
      this.webrtcService.addIceCandidate(data.candidate);
    });

    // Error en llamada
    this.signalrService.onCallFailed((data) => {
      this.endCallLocally();
      alert(data.message || 'No se pudo realizar la llamada');
    });
  }

  async startCall(targetUserId: string, targetUserName: string, isVideo: boolean): Promise<void> {
    try {
      // Inicializar WebRTC
      await this.webrtcService.initializeConnection((candidate) => {
        this.signalrService.sendIceCandidate(targetUserId, candidate);
      });

      // Crear offer
      const sdpOffer = await this.webrtcService.createOffer(isVideo);

      // Actualizar estado
      this._callState.set({
        isInCall: true,
        isIncoming: false,
        isVideo,
        targetUserId,
        targetUserName
      });

      // Registrar llamada en backend
      this.http.post<{ id: string }>(`${this.apiUrl}/calls/start`, {
        receptorId: targetUserId,
        tipo: isVideo ? 'Video' : 'Voz'
      }).subscribe(response => {
        this._callState.update(state => ({ ...state, callId: response.id }));
      });

      // Enviar offer via SignalR
      this.signalrService.callUser(targetUserId, isVideo ? 'Video' : 'Voz', sdpOffer);

    } catch (error) {
      console.error('Error starting call:', error);
      this.endCallLocally();
      throw error;
    }
  }

  async acceptCall(): Promise<void> {
    const state = this._callState();
    if (!state.callerId) return;

    try {
      const sdpOffer = (window as any).__pendingSdpOffer;
      delete (window as any).__pendingSdpOffer;

      // Inicializar WebRTC
      await this.webrtcService.initializeConnection((candidate) => {
        this.signalrService.sendIceCandidate(state.callerId!, candidate);
      });

      // Crear answer
      const sdpAnswer = await this.webrtcService.createAnswer(sdpOffer, state.isVideo);

      // Actualizar estado
      this._callState.update(s => ({
        ...s,
        isIncoming: false,
        startTime: new Date()
      }));

      // Enviar answer
      this.signalrService.answerCall(state.callerId, sdpAnswer);

      this.startDurationTimer();

    } catch (error) {
      console.error('Error accepting call:', error);
      this.rejectCall();
      throw error;
    }
  }

  rejectCall(): void {
    const state = this._callState();
    if (state.callerId) {
      this.signalrService.rejectCall(state.callerId, 'Rejected');
    }
    this.endCallLocally();
  }

  endCall(): void {
    const state = this._callState();
    const otherUserId = state.callerId || state.targetUserId;

    if (otherUserId) {
      this.signalrService.endCall(otherUserId);
    }

    // Actualizar en backend
    if (state.callId) {
      this.http.put(`${this.apiUrl}/calls/${state.callId}/end`, {
        estado: 'Completada'
      }).subscribe();
    }

    this.endCallLocally();
  }

  private endCallLocally(): void {
    this.webrtcService.endCall();
    this._callState.set({
      isInCall: false,
      isIncoming: false,
      isVideo: false
    });
    this.stopDurationTimer();
  }

  private startDurationTimer(): void {
    this.callDuration.set(0);
    this._callState.update(s => ({ ...s, startTime: new Date() }));

    this.durationInterval = window.setInterval(() => {
      this.callDuration.update(d => d + 1);
    }, 1000);
  }

  private stopDurationTimer(): void {
    if (this.durationInterval) {
      clearInterval(this.durationInterval);
      this.durationInterval = null;
    }
    this.callDuration.set(0);
  }

  toggleVideo(): void {
    this.webrtcService.toggleVideo();
  }

  toggleAudio(): void {
    this.webrtcService.toggleAudio();
  }

  formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  getCallHistory(): Observable<CallHistoryItem[]> {
    return this.http.get<CallHistoryItem[]>(`${this.apiUrl}/calls`);
  }
}
