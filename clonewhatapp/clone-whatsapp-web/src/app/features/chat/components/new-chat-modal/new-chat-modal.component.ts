import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContactService } from '../../../../core/services/contact.service';
import { ChatService } from '../../../../core/services/chat.service';
import { Contact } from '../../../../core/models';

@Component({
  selector: 'app-new-chat-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './new-chat-modal.component.html',
  styleUrl: './new-chat-modal.component.scss'
})
export class NewChatModalComponent {
  @Output() close = new EventEmitter<void>();

  activeTab: 'contacts' | 'add' | 'group' = 'contacts';
  newContactPhone = '';
  newContactName = '';
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // Group creation
  groupName = '';
  groupDescription = '';
  selectedContacts: Set<string> = new Set();

  constructor(
    public contactService: ContactService,
    private chatService: ChatService
  ) {
    this.contactService.getContacts().subscribe();
  }

  startChat(contact: Contact): void {
    this.chatService.createIndividualChat({ contactoId: contact.contactoUsuarioId })
      .subscribe({
        next: (chat) => {
          this.chatService.selectChat(chat);
          this.close.emit();
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Error al crear chat';
        }
      });
  }

  addContact(): void {
    if (!this.newContactPhone.trim()) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.contactService.addContact({
      numeroTelefono: this.newContactPhone.trim(),
      nombrePersonalizado: this.newContactName.trim() || undefined
    }).subscribe({
      next: () => {
        this.successMessage = 'Contacto agregado exitosamente';
        this.newContactPhone = '';
        this.newContactName = '';
        this.isLoading = false;
        this.activeTab = 'contacts';
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Error al agregar contacto';
      }
    });
  }

  closeModal(): void {
    this.close.emit();
  }

  // Group methods
  toggleContactSelection(contact: Contact): void {
    const id = contact.contactoUsuarioId;
    if (this.selectedContacts.has(id)) {
      this.selectedContacts.delete(id);
    } else {
      this.selectedContacts.add(id);
    }
  }

  isContactSelected(contact: Contact): boolean {
    return this.selectedContacts.has(contact.contactoUsuarioId);
  }

  createGroup(): void {
    if (!this.groupName.trim() || this.selectedContacts.size === 0) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.chatService.createGroup({
      nombre: this.groupName.trim(),
      descripcion: this.groupDescription.trim() || undefined,
      participantesIds: Array.from(this.selectedContacts)
    }).subscribe({
      next: (chat) => {
        this.chatService.selectChat(chat);
        this.resetGroupForm();
        this.close.emit();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Error al crear el grupo';
      }
    });
  }

  resetGroupForm(): void {
    this.groupName = '';
    this.groupDescription = '';
    this.selectedContacts.clear();
    this.isLoading = false;
  }

  getSelectedContactNames(): string {
    const contacts = this.contactService.contacts();
    return contacts
      .filter(c => this.selectedContacts.has(c.contactoUsuarioId))
      .map(c => c.nombrePersonalizado || c.nombre)
      .join(', ');
  }
}
