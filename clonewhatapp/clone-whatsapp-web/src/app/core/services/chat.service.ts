import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Chat, CreateIndividualChatRequest, CreateGroupRequest, MuteChatRequest, ArchiveChatRequest } from '../models';
import { Message, SendMessageRequest } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly apiUrl = environment.apiUrl;
  private http = inject(HttpClient);

  chats = signal<Chat[]>([]);
  archivedChats = signal<Chat[]>([]);
  selectedChat = signal<Chat | null>(null);
  messages = signal<Message[]>([]);

  // Callback to notify SignalR of selected chat changes (avoids circular dependency)
  private onChatSelectedCallback?: (chatId: string | null) => void;

  setOnChatSelectedCallback(callback: (chatId: string | null) => void): void {
    this.onChatSelectedCallback = callback;
  }

  getChats(): Observable<Chat[]> {
    return this.http.get<Chat[]>(`${this.apiUrl}/chats`)
      .pipe(
        tap(chats => this.chats.set(chats))
      );
  }

  getChat(id: string): Observable<Chat> {
    return this.http.get<Chat>(`${this.apiUrl}/chats/${id}`);
  }

  createIndividualChat(request: CreateIndividualChatRequest): Observable<Chat> {
    return this.http.post<Chat>(`${this.apiUrl}/chats/individual`, request)
      .pipe(
        tap(chat => {
          const currentChats = this.chats();
          if (!currentChats.find(c => c.id === chat.id)) {
            this.chats.set([chat, ...currentChats]);
          }
        })
      );
  }

  createGroup(request: CreateGroupRequest): Observable<Chat> {
    return this.http.post<Chat>(`${this.apiUrl}/chats/grupo`, request)
      .pipe(
        tap(chat => {
          this.chats.set([chat, ...this.chats()]);
        })
      );
  }

  getMessages(chatId: string, page: number = 1, pageSize: number = 50): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/messages/chat/${chatId}?page=${page}&pageSize=${pageSize}`)
      .pipe(
        tap(messages => {
          if (page === 1) {
            this.messages.set(messages);
          } else {
            this.messages.set([...messages, ...this.messages()]);
          }
        })
      );
  }

  sendMessage(request: SendMessageRequest): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/messages`, request)
      .pipe(
        tap(message => {
          this.addMessage(message); // Usa addMessage para evitar duplicados
        })
      );
  }

  sendMessageWithFile(chatId: string, file: File, contenido?: string, duracionSegundos?: number): Observable<Message> {
    const formData = new FormData();
    formData.append('chatId', chatId);
    formData.append('file', file);
    if (contenido) {
      formData.append('contenido', contenido);
    }
    if (duracionSegundos !== undefined) {
      formData.append('duracionSegundos', duracionSegundos.toString());
    }

    return this.http.post<Message>(`${this.apiUrl}/messages/with-file`, formData)
      .pipe(
        tap(message => {
          this.addMessage(message); // Usa addMessage para evitar duplicados
        })
      );
  }

  sendAudioMessage(chatId: string, audioBlob: Blob, duracionSegundos: number): Observable<Message> {
    const file = new File([audioBlob], `audio_${Date.now()}.webm`, { type: 'audio/webm' });
    return this.sendMessageWithFile(chatId, file, undefined, duracionSegundos);
  }

  markAsRead(messageId: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/messages/${messageId}/read`, {});
  }

  markAllAsRead(chatId: string): Observable<{ markedAsRead: number }> {
    return this.http.put<{ markedAsRead: number }>(`${this.apiUrl}/messages/chat/${chatId}/read-all`, {});
  }

  deleteMessage(messageId: string, forEveryone: boolean = false): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/messages/${messageId}?forEveryone=${forEveryone}`);
  }

  updateMessageStatus(messageId: string, status: 'Enviado' | 'Entregado' | 'Leido'): void {
    const messages = this.messages();
    const index = messages.findIndex(m => m.id === messageId);
    if (index !== -1) {
      const updatedMessages = [...messages];
      updatedMessages[index] = { ...updatedMessages[index], estado: status };
      this.messages.set(updatedMessages);
    }
  }

  updateMessagesStatus(messageIds: string[], status: 'Enviado' | 'Entregado' | 'Leido'): void {
    const messages = this.messages();
    const updatedMessages = messages.map(m =>
      messageIds.includes(m.id) ? { ...m, estado: status } : m
    );
    this.messages.set(updatedMessages);
  }

  selectChat(chat: Chat | null): void {
    this.selectedChat.set(chat);
    // Notify SignalR service of chat selection for notification sound logic
    this.onChatSelectedCallback?.(chat?.id ?? null);
    if (chat) {
      this.getMessages(chat.id).subscribe();
    } else {
      this.messages.set([]);
    }
  }

  addMessage(message: Message): void {
    const currentMessages = this.messages();
    if (!currentMessages.find(m => m.id === message.id)) {
      this.messages.set([...currentMessages, message]);
      this.updateChatLastMessage(message);
    }
  }

  searchMessages(chatId: string, query: string, limit: number = 20): Observable<Message[]> {
    return this.http.get<Message[]>(
      `${this.apiUrl}/messages/chat/${chatId}/search?query=${encodeURIComponent(query)}&limit=${limit}`
    );
  }

  forwardMessage(messageId: string, targetChatId: string): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/messages/${messageId}/forward`, { targetChatId });
  }

  // ============ Silenciar/Archivar ============

  getArchivedChats(): Observable<Chat[]> {
    return this.http.get<Chat[]>(`${this.apiUrl}/chats/archivados`)
      .pipe(
        tap(chats => this.archivedChats.set(chats))
      );
  }

  muteChat(chatId: string, duracion: '8h' | '1w' | 'always'): Observable<{ silenciado: boolean; silenciadoHasta: Date | null }> {
    return this.http.put<{ silenciado: boolean; silenciadoHasta: Date | null }>(
      `${this.apiUrl}/chats/${chatId}/silenciar`,
      { silenciar: true, duracion }
    ).pipe(
      tap(result => {
        this.updateChatMuteStatus(chatId, result.silenciado, result.silenciadoHasta);
      })
    );
  }

  unmuteChat(chatId: string): Observable<{ silenciado: boolean; silenciadoHasta: Date | null }> {
    return this.http.put<{ silenciado: boolean; silenciadoHasta: Date | null }>(
      `${this.apiUrl}/chats/${chatId}/silenciar`,
      { silenciar: false }
    ).pipe(
      tap(result => {
        this.updateChatMuteStatus(chatId, result.silenciado, result.silenciadoHasta);
      })
    );
  }

  archiveChat(chatId: string): Observable<{ archivado: boolean }> {
    return this.http.put<{ archivado: boolean }>(
      `${this.apiUrl}/chats/${chatId}/archivar`,
      { archivar: true }
    ).pipe(
      tap(() => {
        const chats = this.chats();
        const chatToArchive = chats.find(c => c.id === chatId);
        if (chatToArchive) {
          this.chats.set(chats.filter(c => c.id !== chatId));
          this.archivedChats.set([{ ...chatToArchive, archivado: true }, ...this.archivedChats()]);
        }
        if (this.selectedChat()?.id === chatId) {
          this.selectChat(null);
        }
      })
    );
  }

  unarchiveChat(chatId: string): Observable<{ archivado: boolean }> {
    return this.http.put<{ archivado: boolean }>(
      `${this.apiUrl}/chats/${chatId}/archivar`,
      { archivar: false }
    ).pipe(
      tap(() => {
        const archived = this.archivedChats();
        const chatToUnarchive = archived.find(c => c.id === chatId);
        if (chatToUnarchive) {
          this.archivedChats.set(archived.filter(c => c.id !== chatId));
          this.chats.set([{ ...chatToUnarchive, archivado: false }, ...this.chats()]);
        }
      })
    );
  }

  private updateChatMuteStatus(chatId: string, silenciado: boolean, silenciadoHasta: Date | null): void {
    const chats = this.chats();
    const index = chats.findIndex(c => c.id === chatId);
    if (index !== -1) {
      const updatedChats = [...chats];
      updatedChats[index] = { ...updatedChats[index], silenciado, silenciadoHasta };
      this.chats.set(updatedChats);
    }

    const archived = this.archivedChats();
    const archivedIndex = archived.findIndex(c => c.id === chatId);
    if (archivedIndex !== -1) {
      const updatedArchived = [...archived];
      updatedArchived[archivedIndex] = { ...updatedArchived[archivedIndex], silenciado, silenciadoHasta };
      this.archivedChats.set(updatedArchived);
    }
  }

  private updateChatLastMessage(message: Message): void {
    const chats = this.chats();
    const chatIndex = chats.findIndex(c => c.id === message.chatId);

    if (chatIndex !== -1) {
      const updatedChats = [...chats];
      updatedChats[chatIndex] = {
        ...updatedChats[chatIndex],
        ultimoMensaje: message,
        ultimaActividad: new Date()
      };

      // Move chat to top
      const [chat] = updatedChats.splice(chatIndex, 1);
      this.chats.set([chat, ...updatedChats]);
    }
  }
}
