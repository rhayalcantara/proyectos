import { Injectable, signal, inject } from '@angular/core';
import * as signalR from '@microsoft/signalr';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { NotificationSettingsService } from './notification-settings.service';
import { ChatService } from './chat.service';
import { MessageSentEvent, TypingEvent, MessageStatusEvent, MessagesReadEvent, Status, StatusViewer } from '../models';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SignalRService {
  private hubConnection?: signalR.HubConnection;

  isConnected = signal<boolean>(false);

  // Event subjects
  messageReceived$ = new Subject<MessageSentEvent>();
  userTyping$ = new Subject<TypingEvent>();
  messageStatusUpdated$ = new Subject<MessageStatusEvent>();
  messagesRead$ = new Subject<MessagesReadEvent>();
  messageDeleted$ = new Subject<{ messageId: string; forEveryone: boolean }>();
  userStatusChanged$ = new Subject<{ userId: string; isOnline: boolean; lastSeen: Date }>();

  // Status events
  newStatus$ = new Subject<Status>();
  statusDeleted$ = new Subject<{ usuarioId: string; estadoId: string }>();
  statusViewed$ = new Subject<{ estadoId: string; viewer: StatusViewer }>();

  // Call events - callbacks
  private callOfferCallback?: (data: any) => void;
  private callAnswerCallback?: (data: any) => void;
  private callRejectedCallback?: (data: any) => void;
  private callEndedCallback?: () => void;
  private iceCandidateCallback?: (data: any) => void;
  private callFailedCallback?: (data: any) => void;

  // Track currently selected chat for notification sound logic
  selectedChatId = signal<string | null>(null);

  private authService = inject(AuthService);
  private notificationSettings = inject(NotificationSettingsService);
  private chatService = inject(ChatService);

  constructor() {
    // Register callback to track selected chat for notification sound logic
    this.chatService.setOnChatSelectedCallback((chatId) => {
      this.selectedChatId.set(chatId);
    });
  }

  async startConnection(): Promise<void> {
    const token = this.authService.getToken();

    if (!token) {
      console.error('No token available for SignalR connection');
      return;
    }

    this.hubConnection = new signalR.HubConnectionBuilder()
      .withUrl(environment.hubUrl, {
        accessTokenFactory: () => token
      })
      .withAutomaticReconnect()
      .build();

    this.registerHandlers();

    try {
      await this.hubConnection.start();
      console.log('SignalR Connected');
      this.isConnected.set(true);
    } catch (err) {
      console.error('SignalR Connection Error:', err);
      this.isConnected.set(false);
    }
  }

  async stopConnection(): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.stop();
      this.isConnected.set(false);
    }
  }

  private registerHandlers(): void {
    if (!this.hubConnection) return;

    this.hubConnection.on('ReceiveMessage', (data: MessageSentEvent) => {
      this.messageReceived$.next(data);

      // Play notification sound if conditions are met
      const currentUser = this.authService.currentUser();
      if (currentUser && data.mensaje && this.notificationSettings.shouldPlaySound(
        data.mensaje.remitenteId,
        currentUser.id,
        this.selectedChatId(),
        data.chatId
      )) {
        this.notificationSettings.playNotificationSound();
      }
    });

    this.hubConnection.on('UserTyping', (data: TypingEvent) => {
      this.userTyping$.next(data);
    });

    this.hubConnection.on('MessageStatusUpdated', (data: MessageStatusEvent) => {
      this.messageStatusUpdated$.next(data);
    });

    this.hubConnection.on('MessagesRead', (data: MessagesReadEvent) => {
      this.messagesRead$.next(data);
    });

    this.hubConnection.on('MessageDeleted', (data: { messageId: string; forEveryone: boolean }) => {
      this.messageDeleted$.next(data);
    });

    this.hubConnection.on('UserStatusChanged', (data: { userId: string; isOnline: boolean; lastSeen: Date }) => {
      this.userStatusChanged$.next(data);
    });

    // Status handlers
    this.hubConnection.on('NewStatus', (data: Status) => {
      this.newStatus$.next(data);
    });

    this.hubConnection.on('StatusDeleted', (data: { usuarioId: string; estadoId: string }) => {
      this.statusDeleted$.next(data);
    });

    this.hubConnection.on('StatusViewed', (data: { estadoId: string; viewer: StatusViewer }) => {
      this.statusViewed$.next(data);
    });

    // Call handlers
    this.hubConnection.on('ReceiveCallOffer', (data: any) => {
      this.callOfferCallback?.(data);
    });

    this.hubConnection.on('ReceiveCallAnswer', (data: any) => {
      this.callAnswerCallback?.(data);
    });

    this.hubConnection.on('CallRejected', (data: any) => {
      this.callRejectedCallback?.(data);
    });

    this.hubConnection.on('CallEnded', () => {
      this.callEndedCallback?.();
    });

    this.hubConnection.on('ReceiveICECandidate', (data: any) => {
      this.iceCandidateCallback?.(data);
    });

    this.hubConnection.on('CallFailed', (data: any) => {
      this.callFailedCallback?.(data);
    });

    this.hubConnection.onreconnecting(() => {
      console.log('SignalR Reconnecting...');
      this.isConnected.set(false);
    });

    this.hubConnection.onreconnected(() => {
      console.log('SignalR Reconnected');
      this.isConnected.set(true);
    });
  }

  async joinChat(chatId: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('JoinChat', chatId);
    }
  }

  async leaveChat(chatId: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('LeaveChat', chatId);
    }
  }

  async sendTyping(chatId: string, isTyping: boolean): Promise<void> {
    if (this.hubConnection) {
      const user = this.authService.currentUser();
      if (user) {
        await this.hubConnection.invoke('SendTyping', {
          chatId,
          usuarioId: user.id,
          nombreUsuario: user.nombre,
          estaEscribiendo: isTyping
        });
      }
    }
  }

  async markMessageAsDelivered(messageId: string, chatId: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('MessageDelivered', messageId, chatId);
    }
  }

  async markMessageAsRead(messageId: string, chatId: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('MessageRead', messageId, chatId);
    }
  }

  // ============ CALL METHODS ============

  onCallOffer(callback: (data: any) => void): void {
    this.callOfferCallback = callback;
  }

  onCallAnswer(callback: (data: any) => void): void {
    this.callAnswerCallback = callback;
  }

  onCallRejected(callback: (data: any) => void): void {
    this.callRejectedCallback = callback;
  }

  onCallEnded(callback: () => void): void {
    this.callEndedCallback = callback;
  }

  onIceCandidate(callback: (data: any) => void): void {
    this.iceCandidateCallback = callback;
  }

  onCallFailed(callback: (data: any) => void): void {
    this.callFailedCallback = callback;
  }

  async callUser(targetUserId: string, callType: string, sdpOffer: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('CallUser', targetUserId, callType, sdpOffer);
    }
  }

  async answerCall(callerId: string, sdpAnswer: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('AnswerCall', callerId, sdpAnswer);
    }
  }

  async rejectCall(callerId: string, reason: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('RejectCall', callerId, reason);
    }
  }

  async endCall(otherUserId: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('EndCall', otherUserId);
    }
  }

  async sendIceCandidate(targetUserId: string, candidate: string): Promise<void> {
    if (this.hubConnection) {
      await this.hubConnection.invoke('SendICECandidate', targetUserId, candidate);
    }
  }
}
