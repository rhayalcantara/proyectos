import { Component, EventEmitter, Output, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  @Output() close = new EventEmitter<void>();

  isEditingName = signal(false);
  isEditingStatus = signal(false);
  isUploading = signal(false);

  editedName = '';
  editedStatus = '';
  errorMessage = signal('');

  private readonly apiBaseUrl = environment.apiUrl.replace('/api', '');

  constructor(public authService: AuthService) {}

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.editedName = user.nombre;
      this.editedStatus = user.estado || 'Hey there! I\'m using WhatsApp Clone';
    }
  }

  getProfileImageUrl(): string | null {
    const user = this.authService.currentUser();
    if (user?.fotoPerfil) {
      if (user.fotoPerfil.startsWith('http')) {
        return user.fotoPerfil;
      }
      return `${this.apiBaseUrl}${user.fotoPerfil}`;
    }
    return null;
  }

  getUserInitial(): string {
    return this.authService.currentUser()?.nombre?.charAt(0).toUpperCase() || 'U';
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.uploadPhoto(input.files[0]);
    }
  }

  uploadPhoto(file: File): void {
    if (this.isUploading()) return;

    const formData = new FormData();
    formData.append('file', file);

    this.isUploading.set(true);
    this.errorMessage.set('');

    this.authService.uploadProfilePhoto(formData).subscribe({
      next: () => {
        this.isUploading.set(false);
      },
      error: (err) => {
        this.isUploading.set(false);
        this.errorMessage.set(err.error?.message || 'Error al subir la foto');
      }
    });
  }

  deletePhoto(): void {
    if (this.isUploading()) return;

    this.isUploading.set(true);
    this.authService.deleteProfilePhoto().subscribe({
      next: () => {
        this.isUploading.set(false);
      },
      error: () => {
        this.isUploading.set(false);
        this.errorMessage.set('Error al eliminar la foto');
      }
    });
  }

  startEditingName(): void {
    const user = this.authService.currentUser();
    this.editedName = user?.nombre || '';
    this.isEditingName.set(true);
  }

  saveName(): void {
    if (!this.editedName.trim()) {
      this.errorMessage.set('El nombre no puede estar vacío');
      return;
    }

    this.authService.updateProfile({ nombre: this.editedName.trim() }).subscribe({
      next: () => {
        this.isEditingName.set(false);
        this.errorMessage.set('');
      },
      error: () => {
        this.errorMessage.set('Error al actualizar el nombre');
      }
    });
  }

  cancelEditName(): void {
    const user = this.authService.currentUser();
    this.editedName = user?.nombre || '';
    this.isEditingName.set(false);
    this.errorMessage.set('');
  }

  startEditingStatus(): void {
    const user = this.authService.currentUser();
    this.editedStatus = user?.estado || '';
    this.isEditingStatus.set(true);
  }

  saveStatus(): void {
    this.authService.updateProfile({ estado: this.editedStatus.trim() }).subscribe({
      next: () => {
        this.isEditingStatus.set(false);
        this.errorMessage.set('');
      },
      error: () => {
        this.errorMessage.set('Error al actualizar el estado');
      }
    });
  }

  cancelEditStatus(): void {
    const user = this.authService.currentUser();
    this.editedStatus = user?.estado || '';
    this.isEditingStatus.set(false);
    this.errorMessage.set('');
  }

  closePanel(): void {
    this.close.emit();
  }
}
