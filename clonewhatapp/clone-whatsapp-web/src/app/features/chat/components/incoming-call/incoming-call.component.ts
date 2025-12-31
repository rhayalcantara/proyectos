import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CallService } from '../../../../core/services/call.service';

@Component({
  selector: 'app-incoming-call',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './incoming-call.component.html',
  styleUrl: './incoming-call.component.scss'
})
export class IncomingCallComponent {
  constructor(public callService: CallService) {}

  get callerName(): string {
    return this.callService.callState().callerName || 'Desconocido';
  }

  get callerPhoto(): string | undefined {
    return this.callService.callState().callerPhoto;
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
