import { Component, ElementRef, ViewChild, AfterViewChecked, OnInit, effect, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../../core/services/chat.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SignalRService } from '../../../../core/services/signalr.service';
import { AudioRecorderService } from '../../../../core/services/audio-recorder.service';
import { CallService } from '../../../../core/services/call.service';
import { BlockService } from '../../../../core/services/block.service';
import { Message, ReplyToMessage, BlockStatus } from '../../../../core/models';
import { environment } from '../../../../../environments/environment';
import { AudioPlayerComponent } from '../audio-player/audio-player.component';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, AudioPlayerComponent],
  templateUrl: './chat-window.component.html',
  styleUrl: './chat-window.component.scss'
})
export class ChatWindowComponent implements AfterViewChecked, OnInit {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  newMessage = '';
  selectedFile: File | null = null;
  isUploading = false;
  replyingTo = signal<ReplyToMessage | null>(null);
  activeMessageMenu = signal<string | null>(null);
  isSearchOpen = signal<boolean>(false);
  searchQuery = '';
  searchResults = signal<Message[]>([]);
  isSearching = false;
  showForwardModal = signal<boolean>(false);
  messageToForward = signal<Message | null>(null);
  showHeaderMenu = signal<boolean>(false);
  blockStatus = signal<BlockStatus | null>(null);
  private shouldScroll = true;
  private lastChatId: string | null = null;
  private readonly apiBaseUrl = environment.apiUrl.replace('/api', '');

  constructor(
    public chatService: ChatService,
    public authService: AuthService,
    private signalRService: SignalRService,
    public audioRecorder: AudioRecorderService,
    private callService: CallService,
    private blockService: BlockService
  ) {
    // Effect para detectar cambios en el chat seleccionado
    effect(() => {
      const chat = this.chatService.selectedChat();
      if (chat && chat.id !== this.lastChatId) {
        this.lastChatId = chat.id;
        // Marcar mensajes como leídos después de un pequeño delay
        setTimeout(() => this.markMessagesAsRead(), 500);
        // Verificar estado de bloqueo para chats individuales
        if (chat.tipo === 'Individual' && chat.otroParticipante) {
          this.checkBlockStatus(chat.otroParticipante.id);
        } else {
          this.blockStatus.set(null);
        }
      }
    });
  }

  ngOnInit(): void {}

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
    }
  }

  private markMessagesAsRead(): void {
    const chat = this.chatService.selectedChat();
    if (chat) {
      this.chatService.markAllAsRead(chat.id).subscribe();
    }
  }

  sendMessage(): void {
    const chat = this.chatService.selectedChat();
    if (!chat) return;

    // Si hay archivo, enviar con archivo
    if (this.selectedFile) {
      this.isUploading = true;
      console.log('Enviando archivo:', this.selectedFile.name, 'Tamaño:', this.selectedFile.size);
      this.chatService.sendMessageWithFile(
        chat.id,
        this.selectedFile,
        this.newMessage.trim() || undefined
      ).subscribe({
        next: () => {
          this.newMessage = '';
          this.selectedFile = null;
          this.isUploading = false;
          this.shouldScroll = true;
        },
        error: (err) => {
          console.error('Error al enviar archivo:', err);
          this.isUploading = false;
          const errorMsg = err.error?.message || 'Error al enviar el archivo';
          alert(errorMsg);
        }
      });
      return;
    }

    // Si solo hay texto
    if (!this.newMessage.trim()) return;

    const replyTo = this.replyingTo();

    this.chatService.sendMessage({
      chatId: chat.id,
      contenido: this.newMessage.trim(),
      tipo: 'Texto',
      mensajeRespondidoId: replyTo?.id
    }).subscribe({
      next: () => {
        this.newMessage = '';
        this.replyingTo.set(null);
        this.shouldScroll = true;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.selectedFile = input.files[0];
    }
    input.value = ''; // Reset para permitir seleccionar el mismo archivo
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  getFileUrl(path: string): string {
    if (path.startsWith('http')) {
      return path;
    }
    return `${this.apiBaseUrl}${path}`;
  }

  openImage(url: string): void {
    window.open(this.getFileUrl(url), '_blank');
  }

  formatFileSize(bytes?: number): string {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // Acciones de mensaje
  toggleMessageMenu(messageId: string): void {
    if (this.activeMessageMenu() === messageId) {
      this.activeMessageMenu.set(null);
    } else {
      this.activeMessageMenu.set(messageId);
    }
  }

  closeMessageMenu(): void {
    this.activeMessageMenu.set(null);
  }

  replyToMessage(message: Message): void {
    this.replyingTo.set({
      id: message.id,
      contenido: message.contenido,
      remitenteNombre: message.remitenteNombre,
      tipo: message.tipo
    });
    this.activeMessageMenu.set(null);
  }

  cancelReply(): void {
    this.replyingTo.set(null);
  }

  copyMessage(message: Message): void {
    if (message.contenido) {
      navigator.clipboard.writeText(message.contenido);
    }
    this.activeMessageMenu.set(null);
  }

  deleteMessage(message: Message, forEveryone: boolean): void {
    this.chatService.deleteMessage(message.id, forEveryone).subscribe({
      next: () => {
        // Actualizar localmente
        const messages = this.chatService.messages();
        if (forEveryone) {
          // Marcar como eliminado para todos
          const updated = messages.map(m =>
            m.id === message.id ? { ...m, eliminadoParaTodos: true, contenido: undefined } : m
          );
          this.chatService.messages.set(updated);
        } else {
          // Eliminar de la lista local
          this.chatService.messages.set(messages.filter(m => m.id !== message.id));
        }
      }
    });
    this.activeMessageMenu.set(null);
  }

  getChatName(): string {
    const chat = this.chatService.selectedChat();
    if (!chat) return '';

    if (chat.tipo === 'Grupo') {
      return chat.nombreGrupo || 'Grupo';
    }
    return chat.otroParticipante?.nombre || 'Usuario';
  }

  isOwnMessage(message: Message): boolean {
    return message.remitenteId === this.authService.currentUser()?.id;
  }

  closeChat(): void {
    this.chatService.selectChat(null);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      const container = this.messagesContainer?.nativeElement;
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    } catch (err) {}
  }

  // Búsqueda
  toggleSearch(): void {
    this.isSearchOpen.set(!this.isSearchOpen());
    if (!this.isSearchOpen()) {
      this.searchQuery = '';
      this.searchResults.set([]);
    }
  }

  onSearchInput(): void {
    if (!this.searchQuery.trim()) {
      this.searchResults.set([]);
      return;
    }

    const chat = this.chatService.selectedChat();
    if (!chat) return;

    this.isSearching = true;
    this.chatService.searchMessages(chat.id, this.searchQuery).subscribe({
      next: (results) => {
        this.searchResults.set(results);
        this.isSearching = false;
      },
      error: () => {
        this.isSearching = false;
      }
    });
  }

  scrollToMessage(messageId: string): void {
    const element = document.getElementById(`message-${messageId}`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      element.classList.add('highlight');
      setTimeout(() => element.classList.remove('highlight'), 2000);
    }
    this.isSearchOpen.set(false);
    this.searchQuery = '';
    this.searchResults.set([]);
  }

  // Reenvío
  openForwardModal(message: Message): void {
    this.messageToForward.set(message);
    this.showForwardModal.set(true);
    this.activeMessageMenu.set(null);
  }

  closeForwardModal(): void {
    this.showForwardModal.set(false);
    this.messageToForward.set(null);
  }

  forwardToChat(targetChatId: string): void {
    const message = this.messageToForward();
    if (!message) return;

    this.chatService.forwardMessage(message.id, targetChatId).subscribe({
      next: () => {
        this.closeForwardModal();
      }
    });
  }

  getAvailableChats() {
    const currentChatId = this.chatService.selectedChat()?.id;
    return this.chatService.chats().filter(c => c.id !== currentChatId);
  }

  getChatDisplayName(chat: any): string {
    if (chat.tipo === 'Grupo') {
      return chat.nombreGrupo || 'Grupo';
    }
    return chat.otroParticipante?.nombre || 'Usuario';
  }

  // Métodos de grabación de audio
  async startRecording(): Promise<void> {
    const started = await this.audioRecorder.startRecording();
    if (!started) {
      alert('No se pudo acceder al micrófono. Verifica los permisos del navegador.');
    }
  }

  cancelRecording(): void {
    this.audioRecorder.cancelRecording();
  }

  async stopAndSendRecording(): Promise<void> {
    const chat = this.chatService.selectedChat();
    if (!chat) return;

    const result = await this.audioRecorder.stopRecording();
    if (result) {
      this.isUploading = true;
      this.chatService.sendAudioMessage(chat.id, result.blob, result.duration).subscribe({
        next: () => {
          this.isUploading = false;
          this.shouldScroll = true;
        },
        error: () => {
          this.isUploading = false;
          alert('Error al enviar el mensaje de voz');
        }
      });
    }
  }

  // Métodos de llamadas
  startVoiceCall(): void {
    const chat = this.chatService.selectedChat();
    if (chat?.otroParticipante) {
      this.callService.startCall(
        chat.otroParticipante.id,
        chat.otroParticipante.nombre,
        false
      );
    }
  }

  startVideoCall(): void {
    const chat = this.chatService.selectedChat();
    if (chat?.otroParticipante) {
      this.callService.startCall(
        chat.otroParticipante.id,
        chat.otroParticipante.nombre,
        true
      );
    }
  }

  // Métodos de bloqueo
  toggleHeaderMenu(): void {
    this.showHeaderMenu.set(!this.showHeaderMenu());
  }

  closeHeaderMenu(): void {
    this.showHeaderMenu.set(false);
  }

  checkBlockStatus(userId: string): void {
    this.blockService.getBlockStatus(userId).subscribe({
      next: (status) => {
        this.blockStatus.set(status);
      },
      error: () => {
        this.blockStatus.set(null);
      }
    });
  }

  blockUser(): void {
    const chat = this.chatService.selectedChat();
    if (chat?.otroParticipante) {
      this.blockService.blockUser(chat.otroParticipante.id).subscribe({
        next: () => {
          this.blockStatus.set({ estaBloqueado: true, meBloquearon: false });
          this.closeHeaderMenu();
        },
        error: (err) => {
          console.error('Error al bloquear usuario:', err);
        }
      });
    }
  }

  unblockUser(): void {
    const chat = this.chatService.selectedChat();
    if (chat?.otroParticipante) {
      this.blockService.unblockUser(chat.otroParticipante.id).subscribe({
        next: () => {
          this.blockStatus.set({ estaBloqueado: false, meBloquearon: false });
          this.closeHeaderMenu();
        },
        error: (err) => {
          console.error('Error al desbloquear usuario:', err);
        }
      });
    }
  }

  isBlocked(): boolean {
    const status = this.blockStatus();
    return status?.estaBloqueado || status?.meBloquearon || false;
  }

  getBlockMessage(): string {
    const status = this.blockStatus();
    if (status?.meBloquearon) {
      return 'Este usuario te ha bloqueado';
    }
    if (status?.estaBloqueado) {
      return 'Has bloqueado a este usuario';
    }
    return '';
  }
}
