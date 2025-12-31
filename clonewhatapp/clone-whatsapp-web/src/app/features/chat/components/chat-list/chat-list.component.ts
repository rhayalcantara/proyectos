import { Component, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../../core/services/chat.service';
import { AuthService } from '../../../../core/services/auth.service';
import { StatusService } from '../../../../core/services/status.service';
import { ThemeService } from '../../../../core/services/theme.service';
import { Chat, ContactStatuses } from '../../../../core/models';
import { NewChatModalComponent } from '../new-chat-modal/new-chat-modal.component';
import { ProfileComponent } from '../../../profile/profile.component';
import { StatusListComponent } from '../../../status/components/status-list/status-list.component';
import { CreateStatusComponent } from '../../../status/components/create-status/create-status.component';
import { StatusViewerComponent } from '../../../status/components/status-viewer/status-viewer.component';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-chat-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    NewChatModalComponent,
    ProfileComponent,
    StatusListComponent,
    CreateStatusComponent,
    StatusViewerComponent
  ],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.scss'
})
export class ChatListComponent {
  searchQuery = '';
  showNewChatModal = signal(false);
  showProfilePanel = signal(false);
  activeTab = signal<'chats' | 'status'>('chats');
  showCreateStatus = signal(false);
  showStatusViewer = signal(false);
  selectedContactForViewer = signal<ContactStatuses | null>(null);

  // Context menu state
  showContextMenu = signal(false);
  contextMenuPosition = signal({ x: 0, y: 0 });
  contextMenuChat = signal<Chat | null>(null);

  // Mute modal state
  showMuteModal = signal(false);
  chatToMute = signal<Chat | null>(null);

  // Archived chats
  showArchivedChats = signal(false);

  private readonly apiBaseUrl = environment.apiUrl.replace('/api', '');

  constructor(
    public chatService: ChatService,
    public authService: AuthService,
    public statusService: StatusService,
    public themeService: ThemeService
  ) {}

  selectChat(chat: Chat): void {
    this.chatService.selectChat(chat);
  }

  getChatName(chat: Chat): string {
    if (chat.tipo === 'Grupo') {
      return chat.nombreGrupo || 'Grupo';
    }
    return chat.otroParticipante?.nombre || 'Usuario';
  }

  getChatImage(chat: Chat): string {
    if (chat.tipo === 'Grupo') {
      return chat.imagenGrupo || 'assets/default-group.png';
    }
    return chat.otroParticipante?.fotoPerfil || 'assets/default-avatar.png';
  }

  getLastMessagePreview(chat: Chat): string {
    if (!chat.ultimoMensaje) return '';
    if (chat.ultimoMensaje.eliminadoParaTodos) return 'Mensaje eliminado';
    if (chat.ultimoMensaje.tipo !== 'Texto') {
      return `📎 ${chat.ultimoMensaje.tipo}`;
    }
    return chat.ultimoMensaje.contenido || '';
  }

  openNewChat(): void {
    this.showNewChatModal.set(true);
  }

  closeNewChatModal(): void {
    this.showNewChatModal.set(false);
  }

  logout(): void {
    this.authService.logout();
  }

  openProfile(): void {
    this.showProfilePanel.set(true);
  }

  closeProfile(): void {
    this.showProfilePanel.set(false);
  }

  getUserProfileImage(): string | null {
    const user = this.authService.currentUser();
    if (user?.fotoPerfil) {
      if (user.fotoPerfil.startsWith('http')) {
        return user.fotoPerfil;
      }
      return `${this.apiBaseUrl}${user.fotoPerfil}`;
    }
    return null;
  }

  // Tab navigation
  setActiveTab(tab: 'chats' | 'status'): void {
    this.activeTab.set(tab);
  }

  // Status methods
  openCreateStatus(): void {
    this.showCreateStatus.set(true);
  }

  closeCreateStatus(): void {
    this.showCreateStatus.set(false);
  }

  openStatusViewer(contact: ContactStatuses): void {
    this.selectedContactForViewer.set(contact);
    this.showStatusViewer.set(true);
  }

  closeStatusViewer(): void {
    this.showStatusViewer.set(false);
    this.selectedContactForViewer.set(null);
    this.statusService.selectContactStatuses(null);
  }

  openMyStatuses(): void {
    // Ver mis propios estados - crear un ContactStatuses con mis datos
    const user = this.authService.currentUser();
    if (user && this.statusService.myStatuses().estados.length > 0) {
      const myContact: ContactStatuses = {
        usuarioId: user.id,
        usuarioNombre: user.nombre,
        usuarioFoto: user.fotoPerfil,
        estados: this.statusService.myStatuses().estados,
        todosVistos: true,
        ultimaActualizacion: new Date()
      };
      this.openStatusViewer(myContact);
    }
  }

  // ============ Context Menu ============

  onChatContextMenu(event: MouseEvent, chat: Chat): void {
    event.preventDefault();
    this.contextMenuChat.set(chat);
    this.contextMenuPosition.set({ x: event.clientX, y: event.clientY });
    this.showContextMenu.set(true);
  }

  closeContextMenu(): void {
    this.showContextMenu.set(false);
    this.contextMenuChat.set(null);
  }

  // ============ Mute ============

  openMuteModal(chat: Chat): void {
    this.chatToMute.set(chat);
    this.showMuteModal.set(true);
    this.closeContextMenu();
  }

  closeMuteModal(): void {
    this.showMuteModal.set(false);
    this.chatToMute.set(null);
  }

  muteChat(duracion: '8h' | '1w' | 'always'): void {
    const chat = this.chatToMute();
    if (chat) {
      this.chatService.muteChat(chat.id, duracion).subscribe(() => {
        this.closeMuteModal();
      });
    }
  }

  unmuteChat(chat: Chat): void {
    this.chatService.unmuteChat(chat.id).subscribe();
    this.closeContextMenu();
  }

  // ============ Archive ============

  archiveChat(chat: Chat): void {
    this.chatService.archiveChat(chat.id).subscribe();
    this.closeContextMenu();
  }

  unarchiveChat(chat: Chat): void {
    this.chatService.unarchiveChat(chat.id).subscribe();
  }

  toggleArchivedChats(): void {
    const show = !this.showArchivedChats();
    this.showArchivedChats.set(show);
    if (show) {
      this.chatService.getArchivedChats().subscribe();
    }
  }

  // Close context menu when clicking outside
  onDocumentClick(event: MouseEvent): void {
    if (this.showContextMenu()) {
      this.closeContextMenu();
    }
  }
}
