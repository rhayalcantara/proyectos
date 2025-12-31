import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface PushSubscriptionKeys {
  p256dh: string;
  auth: string;
}

interface PushSubscriptionDTO {
  endpoint: string;
  keys: PushSubscriptionKeys;
}

@Injectable({
  providedIn: 'root'
})
export class PushNotificationService {
  private apiUrl = environment.apiUrl;
  private swRegistration: ServiceWorkerRegistration | null = null;
  private pushSubscription: PushSubscription | null = null;

  isSupported = signal(false);
  isSubscribed = signal(false);
  permission = signal<NotificationPermission>('default');

  constructor(private http: HttpClient) {
    this.checkSupport();
  }

  private checkSupport(): void {
    this.isSupported.set(
      'serviceWorker' in navigator &&
      'PushManager' in window &&
      'Notification' in window
    );
    if ('Notification' in window) {
      this.permission.set(Notification.permission);
    }
  }

  async init(): Promise<void> {
    if (!this.isSupported()) {
      console.log('[Push] Push notifications not supported');
      return;
    }

    try {
      // Registrar el Service Worker
      this.swRegistration = await navigator.serviceWorker.register('/sw-push.js');
      console.log('[Push] Service Worker registered');

      // Verificar si ya hay una suscripción
      this.pushSubscription = await this.swRegistration.pushManager.getSubscription();
      this.isSubscribed.set(!!this.pushSubscription);

      // Escuchar mensajes del Service Worker
      navigator.serviceWorker.addEventListener('message', (event) => {
        if (event.data?.type === 'NOTIFICATION_CLICK') {
          // Emitir evento para navegación
          window.dispatchEvent(new CustomEvent('push-notification-click', {
            detail: { chatId: event.data.chatId }
          }));
        }
      });

    } catch (error) {
      console.error('[Push] Error initializing:', error);
    }
  }

  async requestPermission(): Promise<boolean> {
    if (!this.isSupported()) {
      return false;
    }

    try {
      const permission = await Notification.requestPermission();
      this.permission.set(permission);
      return permission === 'granted';
    } catch (error) {
      console.error('[Push] Error requesting permission:', error);
      return false;
    }
  }

  async subscribe(): Promise<boolean> {
    if (!this.isSupported() || !this.swRegistration) {
      return false;
    }

    if (this.permission() !== 'granted') {
      const granted = await this.requestPermission();
      if (!granted) {
        return false;
      }
    }

    try {
      // Obtener la clave pública VAPID del servidor
      const vapidResponse = await this.http.get<{ publicKey: string }>(
        `${this.apiUrl}/push/vapid-public-key`
      ).toPromise();

      if (!vapidResponse) {
        throw new Error('No VAPID public key received');
      }

      // Convertir la clave VAPID de base64url a Uint8Array
      const applicationServerKey = this.urlBase64ToUint8Array(vapidResponse.publicKey);

      // Suscribirse a push notifications
      this.pushSubscription = await this.swRegistration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey
      });

      // Extraer las claves de la suscripción
      const p256dh = this.arrayBufferToBase64(this.pushSubscription.getKey('p256dh')!);
      const auth = this.arrayBufferToBase64(this.pushSubscription.getKey('auth')!);

      // Enviar la suscripción al servidor
      const subscriptionDTO: PushSubscriptionDTO = {
        endpoint: this.pushSubscription.endpoint,
        keys: { p256dh, auth }
      };

      await this.http.post(
        `${this.apiUrl}/push/subscribe`,
        subscriptionDTO
      ).toPromise();

      this.isSubscribed.set(true);
      console.log('[Push] Subscribed successfully');
      return true;

    } catch (error) {
      console.error('[Push] Error subscribing:', error);
      return false;
    }
  }

  async unsubscribe(): Promise<boolean> {
    if (!this.pushSubscription) {
      return true;
    }

    try {
      // Cancelar suscripción en el navegador
      await this.pushSubscription.unsubscribe();

      // Notificar al servidor
      await this.http.post(
        `${this.apiUrl}/push/unsubscribe`,
        { endpoint: this.pushSubscription.endpoint }
      ).toPromise();

      this.pushSubscription = null;
      this.isSubscribed.set(false);
      console.log('[Push] Unsubscribed successfully');
      return true;

    } catch (error) {
      console.error('[Push] Error unsubscribing:', error);
      return false;
    }
  }

  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
  }

  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }
}
