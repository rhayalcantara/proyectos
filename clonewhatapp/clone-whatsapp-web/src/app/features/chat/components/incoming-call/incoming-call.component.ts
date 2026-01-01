import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CallService } from '../../../../core/services/call.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-incoming-call',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './incoming-call.component.html',
  styleUrl: './incoming-call.component.scss'
})
export class IncomingCallComponent {
  private apiBaseUrl = environment.apiUrl.replace('/api', '');

  constructor(public callService: CallService) {}

  get callerName(): string {
    return this.callService.callState().callerName || 'Desconocido';
  }

  get callerPhoto(): string | undefined {
    const photo = this.callService.callState().callerPhoto;
    if (!photo) return undefined;
    if (photo.startsWith('http')) return photo;
    return `${this.apiBaseUrl}${photo}`;
  }

  get isVideo(): boolean {
    return this.callService.callState().isVideo;
  }

  accept(): void {
    this.callService.acceptCall();
  }

  reject(): void {
    this.callService.rejectCall();
  }
}
