import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class WebRTCService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;

  remoteStream = signal<MediaStream | null>(null);
  localVideoEnabled = signal<boolean>(true);
  localAudioEnabled = signal<boolean>(true);

  private readonly iceServers: RTCIceServer[] = [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
    { urls: 'stun:stun2.l.google.com:19302' }
  ];

  private onIceCandidate: ((candidate: string) => void) | null = null;

  async initializeConnection(onIceCandidate: (candidate: string) => void): Promise<void> {
    this.onIceCandidate = onIceCandidate;

    this.peerConnection = new RTCPeerConnection({
      iceServers: this.iceServers
    });

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate && this.onIceCandidate) {
        this.onIceCandidate(JSON.stringify(event.candidate));
      }
    };

    this.peerConnection.ontrack = (event) => {
      this.remoteStream.set(event.streams[0]);
    };

    this.peerConnection.onconnectionstatechange = () => {
      console.log('Connection state:', this.peerConnection?.connectionState);
    };
  }

  async getLocalStream(isVideo: boolean): Promise<MediaStream> {
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: true,
      video: isVideo ? {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        facingMode: 'user'
      } : false
    });

    return this.localStream;
  }

  async createOffer(isVideo: boolean): Promise<string> {
    if (!this.peerConnection) {
      throw new Error('Peer connection not initialized');
    }

    // Obtener stream local
    const stream = await this.getLocalStream(isVideo);

    // Agregar tracks al peer connection
    stream.getTracks().forEach(track => {
      this.peerConnection!.addTrack(track, stream);
    });

    // Crear y establecer offer
    const offer = await this.peerConnection.createOffer();
    await this.peerConnection.setLocalDescription(offer);

    return JSON.stringify(offer);
  }

  async createAnswer(sdpOffer: string, isVideo: boolean): Promise<string> {
    if (!this.peerConnection) {
      throw new Error('Peer connection not initialized');
    }

    // Obtener stream local
    const stream = await this.getLocalStream(isVideo);

    // Agregar tracks al peer connection
    stream.getTracks().forEach(track => {
      this.peerConnection!.addTrack(track, stream);
    });

    // Establecer remote description (offer recibida)
    const offer = JSON.parse(sdpOffer);
    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(offer));

    // Crear answer
    const answer = await this.peerConnection.createAnswer();
    await this.peerConnection.setLocalDescription(answer);

    return JSON.stringify(answer);
  }

  async handleAnswer(sdpAnswer: string): Promise<void> {
    if (!this.peerConnection) return;

    const answer = JSON.parse(sdpAnswer);
    await this.peerConnection.setRemoteDescription(new RTCSessionDescription(answer));
  }

  async addIceCandidate(candidateStr: string): Promise<void> {
    if (!this.peerConnection) return;

    try {
      const candidate = JSON.parse(candidateStr);
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate));
    } catch (error) {
      console.error('Error adding ICE candidate:', error);
    }
  }

  toggleVideo(): void {
    if (this.localStream) {
      const videoTrack = this.localStream.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled;
        this.localVideoEnabled.set(videoTrack.enabled);
      }
    }
  }

  toggleAudio(): void {
    if (this.localStream) {
      const audioTrack = this.localStream.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        this.localAudioEnabled.set(audioTrack.enabled);
      }
    }
  }

  getLocalStream2(): MediaStream | null {
    return this.localStream;
  }

  endCall(): void {
    // Detener todos los tracks locales
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => track.stop());
      this.localStream = null;
    }

    // Cerrar peer connection
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }

    // Reset signals
    this.remoteStream.set(null);
    this.localVideoEnabled.set(true);
    this.localAudioEnabled.set(true);
    this.onIceCandidate = null;
  }

  async checkPermissions(): Promise<{ audio: boolean; video: boolean }> {
    try {
      const audioResult = await navigator.permissions.query({ name: 'microphone' as PermissionName });
      const videoResult = await navigator.permissions.query({ name: 'camera' as PermissionName });

      return {
        audio: audioResult.state === 'granted',
        video: videoResult.state === 'granted'
      };
    } catch {
      // Fallback: intentar obtener stream
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
        stream.getTracks().forEach(track => track.stop());
        return { audio: true, video: true };
      } catch {
        return { audio: false, video: false };
      }
    }
  }
}
