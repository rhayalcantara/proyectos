import { Component, OnInit, signal, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatusService } from '../../../../core/services/status.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ContactStatuses } from '../../../../core/models';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-status-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-list.component.html',
  styleUrl: './status-list.component.scss'
})
export class StatusListComponent implements OnInit {
  openCreateStatus = output<void>();
  openViewer = output<ContactStatuses>();
  openMyStatuses = output<void>();

  showMyStatuses = signal<boolean>(false);
  private apiUrl = environment.apiUrl.replace('/api', '');

  constructor(
    public statusService: StatusService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadStatuses();
  }

  loadStatuses(): void {
    this.statusService.getMyStatuses().subscribe();
    this.statusService.getContactsStatuses().subscribe();
  }

  getProfileImage(): string | null {
    const foto = this.authService.currentUser()?.fotoPerfil;
    if (foto) {
      return foto.startsWith('http') ? foto : `${this.apiUrl}${foto}`;
    }
    return null;
  }

  getContactImage(contact: ContactStatuses): string | null {
    if (contact.usuarioFoto) {
      return contact.usuarioFoto.startsWith('http')
        ? contact.usuarioFoto
        : `${this.apiUrl}${contact.usuarioFoto}`;
    }
    return null;
  }

  onCreateStatus(): void {
    this.openCreateStatus.emit();
  }

  onViewMyStatuses(): void {
    this.openMyStatuses.emit();
  }

  onViewContactStatuses(contact: ContactStatuses): void {
    this.statusService.selectContactStatuses(contact);
    this.openViewer.emit(contact);
  }

  formatTime(date: Date): string {
    const d = new Date(date);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));

    if (hours < 1) {
      const minutes = Math.floor(diff / (1000 * 60));
      return `hace ${minutes} min`;
    } else if (hours < 24) {
      return `hace ${hours}h`;
    } else {
      return d.toLocaleDateString();
    }
  }

  getSegmentDasharray(total: number): string {
    const circumference = 2 * Math.PI * 22;
    const gap = total > 1 ? 4 : 0;
    const segmentSize = (circumference - gap * total) / total;
    return `${segmentSize} ${circumference - segmentSize}`;
  }

  getSegmentOffset(total: number, index: number): number {
    const circumference = 2 * Math.PI * 22;
    const gap = total > 1 ? 4 : 0;
    const segmentSize = (circumference - gap * total) / total;
    // Rotar para empezar arriba (-90deg = circumference/4)
    return -(index * (segmentSize + gap)) + (circumference / 4);
  }
}
