import { Component, Input, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-audio-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './audio-player.component.html',
  styleUrl: './audio-player.component.scss'
})
export class AudioPlayerComponent implements OnDestroy {
  @Input() audioUrl!: string;
  @Input() duration?: number;

  private audio: HTMLAudioElement | null = null;
  private readonly apiBaseUrl = environment.apiUrl.replace('/api', '');

  isPlaying = signal<boolean>(false);
  currentTime = signal<number>(0);
  totalDuration = signal<number>(0);
  playbackRate = signal<number>(1);

  progress = computed(() => {
    const total = this.totalDuration();
    if (total === 0) return 0;
    return (this.currentTime() / total) * 100;
  });

  ngOnDestroy(): void {
    this.cleanup();
  }

  private getFullUrl(): string {
    if (this.audioUrl.startsWith('http')) {
      return this.audioUrl;
    }
    return `${this.apiBaseUrl}${this.audioUrl}`;
  }

  private initAudio(): void {
    if (!this.audio) {
      this.audio = new Audio(this.getFullUrl());

      this.audio.addEventListener('loadedmetadata', () => {
        this.totalDuration.set(this.audio?.duration || this.duration || 0);
      });

      this.audio.addEventListener('timeupdate', () => {
        this.currentTime.set(this.audio?.currentTime || 0);
      });

      this.audio.addEventListener('ended', () => {
        this.isPlaying.set(false);
        this.currentTime.set(0);
        if (this.audio) {
          this.audio.currentTime = 0;
        }
      });

      this.audio.addEventListener('error', () => {
        console.error('Error al cargar audio');
        this.isPlaying.set(false);
      });

      // Si tenemos duración del backend, usarla
      if (this.duration) {
        this.totalDuration.set(this.duration);
      }
    }
  }

  togglePlay(): void {
    this.initAudio();

    if (!this.audio) return;

    if (this.isPlaying()) {
      this.audio.pause();
      this.isPlaying.set(false);
    } else {
      this.audio.playbackRate = this.playbackRate();
      this.audio.play().catch(err => console.error('Error al reproducir:', err));
      this.isPlaying.set(true);
    }
  }

  seek(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const percent = (event.clientX - rect.left) / rect.width;
    const newTime = percent * this.totalDuration();

    if (this.audio) {
      this.audio.currentTime = newTime;
      this.currentTime.set(newTime);
    }
  }

  changeSpeed(): void {
    const speeds = [1, 1.5, 2];
    const currentIndex = speeds.indexOf(this.playbackRate());
    const nextIndex = (currentIndex + 1) % speeds.length;
    this.playbackRate.set(speeds[nextIndex]);

    if (this.audio) {
      this.audio.playbackRate = this.playbackRate();
    }
  }

  formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  private cleanup(): void {
    if (this.audio) {
      this.audio.pause();
      this.audio.src = '';
      this.audio = null;
    }
  }

  // Generar barras de forma de onda estáticas (simuladas)
  getWaveformBars(): number[] {
    // Generar 30 barras con alturas aleatorias pero consistentes
    const seed = this.audioUrl.split('').reduce((a, b) => a + b.charCodeAt(0), 0);
    const bars: number[] = [];
    for (let i = 0; i < 30; i++) {
      // Usar semilla para generar alturas consistentes
      const height = 20 + ((seed * (i + 1) * 13) % 80);
      bars.push(height);
    }
    return bars;
  }
}
