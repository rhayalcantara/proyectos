import { Component, OnInit, OnDestroy, signal, output, input, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusService } from '../../../../core/services/status.service';
import { ContactStatuses, Status } from '../../../../core/models';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-status-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-viewer.component.html',
  styleUrl: './status-viewer.component.scss'
})
export class StatusViewerComponent implements OnInit, OnDestroy {
  contact = input.required<ContactStatuses>();
  close = output<void>();

  private progressInterval?: number;
  private progressDuration = 5000; // 5 seconds per status
  progress = signal<number>(0);
  isPaused = signal<boolean>(false);
  private apiUrl = environment.apiUrl.replace('/api', '');

  constructor(public statusService: StatusService) {}

  ngOnInit(): void {
    this.startProgress();
    this.markCurrentAsViewed();
  }

  ngOnDestroy(): void {
    this.stopProgress();
  }

  get currentStatus(): Status | null {
    const contact = this.contact();
    const index = this.statusService.currentStatusIndex();
    return contact.estados[index] || null;
  }

  get totalStatuses(): number {
    return this.contact().estados.length;
  }

  get currentIndex(): number {
    return this.statusService.currentStatusIndex();
  }

  private startProgress(): void {
    const startTime = Date.now();

    this.progressInterval = window.setInterval(() => {
      if (this.isPaused()) return;

      const elapsed = Date.now() - startTime;
      const progressValue = (elapsed / this.progressDuration) * 100;

      if (progressValue >= 100) {
        this.goNext();
      } else {
        this.progress.set(progressValue);
      }
    }, 50);
  }

  private stopProgress(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
  }

  private resetProgress(): void {
    this.stopProgress();
    this.progress.set(0);
    this.startProgress();
    this.markCurrentAsViewed();
  }

  private markCurrentAsViewed(): void {
    const status = this.currentStatus;
    if (status && !status.vioPorMi) {
      this.statusService.markAsViewed(status.id).subscribe();
    }
  }

  goNext(): void {
    if (this.statusService.nextStatus()) {
      this.resetProgress();
    } else {
      // No more statuses, close viewer
      this.close.emit();
    }
  }

  goPrevious(): void {
    if (this.statusService.previousStatus()) {
      this.resetProgress();
    }
  }

  onLeftClick(event: MouseEvent): void {
    event.stopPropagation();
    this.goPrevious();
  }

  onRightClick(event: MouseEvent): void {
    event.stopPropagation();
    this.goNext();
  }

  onClose(): void {
    this.close.emit();
  }

  @HostListener('mousedown')
  onMouseDown(): void {
    this.isPaused.set(true);
  }

  @HostListener('mouseup')
  onMouseUp(): void {
    this.isPaused.set(false);
  }

  @HostListener('touchstart')
  onTouchStart(): void {
    this.isPaused.set(true);
  }

  @HostListener('touchend')
  onTouchEnd(): void {
    this.isPaused.set(false);
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    switch (event.key) {
      case 'ArrowLeft':
        this.goPrevious();
        break;
      case 'ArrowRight':
      case ' ':
        this.goNext();
        break;
      case 'Escape':
        this.close.emit();
        break;
    }
  }

  getImageUrl(url: string): string {
    if (url.startsWith('http')) return url;
    return `${this.apiUrl}${url}`;
  }

  getContactImage(): string | null {
    const contact = this.contact();
    if (contact.usuarioFoto) {
      return contact.usuarioFoto.startsWith('http')
        ? contact.usuarioFoto
        : `${this.apiUrl}${contact.usuarioFoto}`;
    }
    return null;
  }

  formatTime(date: Date): string {
    const d = new Date(date);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}
