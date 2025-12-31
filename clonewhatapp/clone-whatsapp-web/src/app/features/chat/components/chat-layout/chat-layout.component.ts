import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatListComponent } from '../chat-list/chat-list.component';
import { ChatWindowComponent } from '../chat-window/chat-window.component';
import { IncomingCallComponent } from '../incoming-call/incoming-call.component';
import { ActiveCallComponent } from '../active-call/active-call.component';
import { ChatService } from '../../../../core/services/chat.service';
import { SignalRService } from '../../../../core/services/signalr.service';
import { ContactService } from '../../../../core/services/contact.service';
import { CallService } from '../../../../core/services/call.service';
import { PushNotificationService } from '../../../../core/services/push-notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-layout',
  standalone: true,
  imports: [CommonModule, ChatListComponent, ChatWindowComponent, IncomingCallComponent, ActiveCallComponent],
  templateUrl: './chat-layout.component.html',
  styleUrl: './chat-layout.component.scss'
})
export class ChatLayoutComponent implements OnInit, OnDestroy {
  private subscriptions: Subscription[] = [];

  constructor(
    public chatService: ChatService,
    private signalRService: SignalRService,
    private contactService: ContactService,
    public callService: CallService,
    private pushService: PushNotificationService
  ) {}

  ngOnInit(): void {
    // Load initial data
    this.chatService.getChats().subscribe();
    this.contactService.getContacts().subscribe();

    // Connect to SignalR
    this.signalRService.startConnection();

    // Initialize push notifications
    this.initPushNotifications();

    // Subscribe to real-time events
    this.subscriptions.push(
      this.signalRService.messageReceived$.subscribe(event => {
        this.chatService.addMessage(event.mensaje);
      }),

      // Estado de mensaje individual actualizado
      this.signalRService.messageStatusUpdated$.subscribe(event => {
        this.chatService.updateMessageStatus(
          event.messageId,
          event.status as 'Enviado' | 'Entregado' | 'Leido'
        );
      }),

      // Múltiples mensajes marcados como leídos
      this.signalRService.messagesRead$.subscribe(event => {
        this.chatService.updateMessagesStatus(event.messageIds, 'Leido');
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.signalRService.stopConnection();
  }

  private async initPushNotifications(): Promise<void> {
    if (!this.pushService.isSupported()) {
      console.log('[Push] Push notifications not supported in this browser');
      return;
    }

    // Inicializar el service worker
    await this.pushService.init();

    // Si el usuario ya otorgó permiso, suscribirse automáticamente
    if (this.pushService.permission() === 'granted' && !this.pushService.isSubscribed()) {
      await this.pushService.subscribe();
    }
    // Si el permiso es 'default', solicitar permiso después de un breve delay
    else if (this.pushService.permission() === 'default') {
      // Esperar 3 segundos antes de solicitar permiso
      setTimeout(async () => {
        const granted = await this.pushService.requestPermission();
        if (granted) {
          await this.pushService.subscribe();
        }
      }, 3000);
    }

    // Escuchar clicks en notificaciones para navegar al chat
    window.addEventListener('push-notification-click', (event: any) => {
      const chatId = event.detail?.chatId;
      if (chatId) {
        const chat = this.chatService.chats().find(c => c.id === chatId);
        if (chat) {
          this.chatService.selectChat(chat);
        }
      }
    });
  }
}
