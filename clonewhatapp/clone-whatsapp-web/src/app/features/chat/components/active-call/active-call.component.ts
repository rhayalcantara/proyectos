import { Component, ElementRef, ViewChild, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CallService } from '../../../../core/services/call.service';
import { WebRTCService } from '../../../../core/services/webrtc.service';

@Component({
  selector: 'app-active-call',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './active-call.component.html',
  styleUrl: './active-call.component.scss'
})
export class ActiveCallComponent implements AfterViewInit, OnDestroy {
  @ViewChild('remoteVideo') remoteVideoRef!: ElementRef<HTMLVideoElement>;
  @ViewChild('localVideo') localVideoRef!: ElementRef<HTMLVideoElement>;

  constructor(
    public callService: CallService,
    public webrtcService: WebRTCService
  ) {
    // Effect para actualizar el video remoto cuando cambie
    effect(() => {
      const remoteStream = this.webrtcService.remoteStream();
      if (remoteStream && this.remoteVideoRef?.nativeElement) {
        console.log('Effect: Asignando remote stream al video');
        this.remoteVideoRef.nativeElement.srcObject = remoteStream;
      }
    });
  }

  ngAfterViewInit(): void {
    // Configurar video local
    const localStream = this.webrtcService.getLocalStream2();
    if (localStream && this.localVideoRef?.nativeElement) {
      this.localVideoRef.nativeElement.srcObject = localStream;
    }

    // Configurar video remoto si ya está disponible
    const remoteStream = this.webrtcService.remoteStream();
    if (remoteStream && this.remoteVideoRef?.nativeElement) {
      console.log('ngAfterViewInit: Asignando remote stream al video');
      this.remoteVideoRef.nativeElement.srcObject = remoteStream;
    }
  }

  ngOnDestroy(): void {
    // Limpiar videos
    if (this.remoteVideoRef?.nativeElement) {
      this.remoteVideoRef.nativeElement.srcObject = null;
    }
    if (this.localVideoRef?.nativeElement) {
      this.localVideoRef.nativeElement.srcObject = null;
    }
  }

  get otherUserName(): string {
    const state = this.callService.callState();
    return state.callerName || state.targetUserName || 'Usuario';
  }

  get isVideo(): boolean {
    return this.callService.callState().isVideo;
  }

  get duration(): string {
    return this.callService.formatDuration(this.callService.callDuration());
  }

  get isAudioEnabled(): boolean {
    return this.webrtcService.localAudioEnabled();
  }

  get isVideoEnabled(): boolean {
    return this.webrtcService.localVideoEnabled();
  }

  toggleMute(): void {
    this.callService.toggleAudio();
  }

  toggleVideo(): void {
    this.callService.toggleVideo();
  }

  endCall(): void {
    this.callService.endCall();
  }
}
