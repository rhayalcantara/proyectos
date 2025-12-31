import { Injectable, signal } from '@angular/core';

export interface NotificationSettings {
  soundEnabled: boolean;
  soundType: 'whatsapp' | 'soft' | 'discrete' | 'none';
  volume: number; // 0-100
  playWhenFocused: boolean;
}

export interface SoundOption {
  id: string;
  name: string;
  type: 'whatsapp' | 'soft' | 'discrete' | 'none';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationSettingsService {
  private readonly STORAGE_KEY = 'notification_settings';
  private audioContext: AudioContext | null = null;

  settings = signal<NotificationSettings>(this.loadSettings());

  readonly soundOptions: SoundOption[] = [
    { id: 'whatsapp', name: 'WhatsApp Clásico', type: 'whatsapp' },
    { id: 'soft', name: 'Tono Suave', type: 'soft' },
    { id: 'discrete', name: 'Tono Discreto', type: 'discrete' },
    { id: 'none', name: 'Sin sonido', type: 'none' }
  ];

  private loadSettings(): NotificationSettings {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored) {
      try {
        return JSON.parse(stored);
      } catch {
        return this.getDefaultSettings();
      }
    }
    return this.getDefaultSettings();
  }

  private getDefaultSettings(): NotificationSettings {
    return {
      soundEnabled: true,
      soundType: 'whatsapp',
      volume: 70,
      playWhenFocused: false
    };
  }

  updateSettings(settings: Partial<NotificationSettings>): void {
    const current = this.settings();
    const updated = { ...current, ...settings };
    this.settings.set(updated);
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(updated));
  }

  async playNotificationSound(): Promise<void> {
    const settings = this.settings();

    if (!settings.soundEnabled || settings.soundType === 'none') {
      return;
    }

    // Check if should play when focused
    if (!settings.playWhenFocused && document.hasFocus()) {
      return;
    }

    try {
      await this.playGeneratedSound(settings.soundType, settings.volume);
    } catch (error) {
      console.log('Could not play notification sound:', error);
    }
  }

  async playTestSound(): Promise<void> {
    const settings = this.settings();
    if (settings.soundType === 'none') return;

    try {
      await this.playGeneratedSound(settings.soundType, settings.volume);
    } catch (error) {
      console.log('Could not play test sound:', error);
    }
  }

  private async playGeneratedSound(type: string, volume: number): Promise<void> {
    // Initialize AudioContext on user interaction
    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }

    // Resume if suspended (browser autoplay policy)
    if (this.audioContext.state === 'suspended') {
      await this.audioContext.resume();
    }

    const ctx = this.audioContext;
    const gainNode = ctx.createGain();
    gainNode.connect(ctx.destination);
    gainNode.gain.value = volume / 100;

    switch (type) {
      case 'whatsapp':
        await this.playWhatsAppTone(ctx, gainNode);
        break;
      case 'soft':
        await this.playSoftTone(ctx, gainNode);
        break;
      case 'discrete':
        await this.playDiscreteTone(ctx, gainNode);
        break;
    }
  }

  private async playWhatsAppTone(ctx: AudioContext, gainNode: GainNode): Promise<void> {
    // Two-tone ascending notification (similar to WhatsApp)
    const frequencies = [523.25, 659.25]; // C5, E5
    const duration = 0.15;

    for (let i = 0; i < frequencies.length; i++) {
      const oscillator = ctx.createOscillator();
      const noteGain = ctx.createGain();

      oscillator.connect(noteGain);
      noteGain.connect(gainNode);

      oscillator.type = 'sine';
      oscillator.frequency.value = frequencies[i];

      const startTime = ctx.currentTime + (i * duration);
      noteGain.gain.setValueAtTime(0.3, startTime);
      noteGain.gain.exponentialRampToValueAtTime(0.01, startTime + duration);

      oscillator.start(startTime);
      oscillator.stop(startTime + duration);
    }
  }

  private async playSoftTone(ctx: AudioContext, gainNode: GainNode): Promise<void> {
    // Soft bell-like sound
    const oscillator = ctx.createOscillator();
    const noteGain = ctx.createGain();

    oscillator.connect(noteGain);
    noteGain.connect(gainNode);

    oscillator.type = 'sine';
    oscillator.frequency.value = 880; // A5

    noteGain.gain.setValueAtTime(0.3, ctx.currentTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.5);

    oscillator.start(ctx.currentTime);
    oscillator.stop(ctx.currentTime + 0.5);
  }

  private async playDiscreteTone(ctx: AudioContext, gainNode: GainNode): Promise<void> {
    // Quick subtle click
    const oscillator = ctx.createOscillator();
    const noteGain = ctx.createGain();

    oscillator.connect(noteGain);
    noteGain.connect(gainNode);

    oscillator.type = 'sine';
    oscillator.frequency.value = 1200;

    noteGain.gain.setValueAtTime(0.2, ctx.currentTime);
    noteGain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08);

    oscillator.start(ctx.currentTime);
    oscillator.stop(ctx.currentTime + 0.1);
  }

  // Check if we should play sound for a message
  shouldPlaySound(messageRemitenteId: string, currentUserId: string, selectedChatId: string | null, messageChatId: string): boolean {
    const settings = this.settings();

    // Sound disabled
    if (!settings.soundEnabled || settings.soundType === 'none') {
      return false;
    }

    // Don't play for own messages
    if (messageRemitenteId === currentUserId) {
      return false;
    }

    // Don't play if viewing that chat (unless playWhenFocused is true)
    if (selectedChatId === messageChatId && !settings.playWhenFocused) {
      return false;
    }

    return true;
  }
}
