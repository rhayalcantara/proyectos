import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AudioRecorderService {
  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];
  private startTime = 0;
  private timerInterval: number | null = null;

  isRecording = signal<boolean>(false);
  recordingDuration = signal<number>(0);
  hasPermission = signal<boolean | null>(null);

  async requestPermission(): Promise<boolean> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach(track => track.stop());
      this.hasPermission.set(true);
      return true;
    } catch {
      this.hasPermission.set(false);
      return false;
    }
  }

  async startRecording(): Promise<boolean> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });

      // Usar webm/opus si está disponible, sino usar audio/webm
      const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm';

      this.mediaRecorder = new MediaRecorder(stream, { mimeType });
      this.audioChunks = [];
      this.startTime = Date.now();

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.start(100); // Capturar cada 100ms
      this.isRecording.set(true);
      this.recordingDuration.set(0);

      // Iniciar timer para actualizar duración
      this.timerInterval = window.setInterval(() => {
        const elapsed = Math.floor((Date.now() - this.startTime) / 1000);
        this.recordingDuration.set(elapsed);
      }, 100);

      return true;
    } catch (error) {
      console.error('Error al iniciar grabación:', error);
      this.hasPermission.set(false);
      return false;
    }
  }

  async stopRecording(): Promise<{ blob: Blob; duration: number } | null> {
    return new Promise((resolve) => {
      if (!this.mediaRecorder || this.mediaRecorder.state === 'inactive') {
        resolve(null);
        return;
      }

      this.mediaRecorder.onstop = () => {
        const duration = this.recordingDuration();
        const blob = new Blob(this.audioChunks, { type: 'audio/webm' });

        // Detener tracks del stream
        this.mediaRecorder?.stream.getTracks().forEach(track => track.stop());

        this.cleanup();
        resolve({ blob, duration });
      };

      this.mediaRecorder.stop();
    });
  }

  cancelRecording(): void {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stream.getTracks().forEach(track => track.stop());
      this.mediaRecorder.stop();
    }
    this.cleanup();
  }

  private cleanup(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.isRecording.set(false);
    this.recordingDuration.set(0);
    this.audioChunks = [];
    this.mediaRecorder = null;
  }

  formatDuration(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
}
