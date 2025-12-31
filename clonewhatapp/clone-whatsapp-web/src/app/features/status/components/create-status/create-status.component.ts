import { Component, signal, output, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StatusService } from '../../../../core/services/status.service';

@Component({
  selector: 'app-create-status',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './create-status.component.html',
  styleUrl: './create-status.component.scss'
})
export class CreateStatusComponent {
  close = output<void>();

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('textInput') textInput!: ElementRef<HTMLTextAreaElement>;

  mode = signal<'select' | 'text' | 'image'>('select');
  textContent = signal<string>('');
  selectedColor = signal<string>('#128C7E');
  selectedImage = signal<File | null>(null);
  imagePreview = signal<string>('');
  imageCaption = signal<string>('');
  isSubmitting = signal<boolean>(false);

  colors = [
    '#128C7E', // WhatsApp green
    '#075E54', // Dark green
    '#25D366', // Light green
    '#34B7F1', // Blue
    '#9C27B0', // Purple
    '#E91E63', // Pink
    '#FF5722', // Orange
    '#795548', // Brown
    '#607D8B', // Gray blue
    '#000000'  // Black
  ];

  constructor(private statusService: StatusService) {}

  selectMode(mode: 'text' | 'image'): void {
    this.mode.set(mode);
    if (mode === 'image') {
      setTimeout(() => this.fileInput.nativeElement.click(), 100);
    } else if (mode === 'text') {
      setTimeout(() => this.textInput?.nativeElement?.focus(), 100);
    }
  }

  selectColor(color: string): void {
    this.selectedColor.set(color);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];

      // Validar tipo
      if (!file.type.startsWith('image/')) {
        alert('Solo se permiten imágenes');
        this.mode.set('select');
        return;
      }

      // Validar tamaño (10MB)
      if (file.size > 10 * 1024 * 1024) {
        alert('La imagen no puede superar 10MB');
        this.mode.set('select');
        return;
      }

      this.selectedImage.set(file);

      // Crear preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.imagePreview.set(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    } else {
      this.mode.set('select');
    }
  }

  submitTextStatus(): void {
    const content = this.textContent().trim();
    if (!content) return;

    this.isSubmitting.set(true);

    this.statusService.createTextStatus({
      contenido: content,
      colorFondo: this.selectedColor()
    }).subscribe({
      next: () => {
        this.close.emit();
      },
      error: (err) => {
        console.error('Error creating status:', err);
        alert('Error al publicar estado');
        this.isSubmitting.set(false);
      }
    });
  }

  submitImageStatus(): void {
    const image = this.selectedImage();
    if (!image) return;

    this.isSubmitting.set(true);

    this.statusService.createImageStatus(image, this.imageCaption() || undefined).subscribe({
      next: () => {
        this.close.emit();
      },
      error: (err) => {
        console.error('Error creating status:', err);
        alert('Error al publicar estado');
        this.isSubmitting.set(false);
      }
    });
  }

  goBack(): void {
    if (this.mode() === 'select') {
      this.close.emit();
    } else {
      this.mode.set('select');
      this.textContent.set('');
      this.selectedImage.set(null);
      this.imagePreview.set('');
      this.imageCaption.set('');
    }
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-overlay')) {
      this.close.emit();
    }
  }
}
