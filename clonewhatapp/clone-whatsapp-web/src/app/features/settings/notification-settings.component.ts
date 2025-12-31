import { Component, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NotificationSettingsService } from '../../core/services/notification-settings.service';

@Component({
  selector: 'app-notification-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notification-settings.component.html',
  styleUrl: './notification-settings.component.scss'
})
export class NotificationSettingsComponent {
  private notificationSettings = inject(NotificationSettingsService);

  close = output<void>();

  settings = this.notificationSettings.settings;
  soundOptions = this.notificationSettings.soundOptions;

  get soundEnabled(): boolean {
    return this.settings().soundEnabled;
  }

  set soundEnabled(value: boolean) {
    this.notificationSettings.updateSettings({ soundEnabled: value });
  }

  get soundType(): string {
    return this.settings().soundType;
  }

  set soundType(value: string) {
    this.notificationSettings.updateSettings({ soundType: value as any });
  }

  get volume(): number {
    return this.settings().volume;
  }

  set volume(value: number) {
    this.notificationSettings.updateSettings({ volume: value });
  }

  get playWhenFocused(): boolean {
    return this.settings().playWhenFocused;
  }

  set playWhenFocused(value: boolean) {
    this.notificationSettings.updateSettings({ playWhenFocused: value });
  }

  testSound(): void {
    this.notificationSettings.playTestSound();
  }

  onClose(): void {
    this.close.emit();
  }
}
