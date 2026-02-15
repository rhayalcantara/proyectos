# Plan de Arquitectura - WhatsApp Clone Mobile App

**Fecha:** 2026-02-14
**Objetivo:** Desarrollar una aplicación móvil multiplataforma (Android/iOS) para el clon de WhatsApp

---

## RESUMEN EJECUTIVO

### Recomendación Principal: **REACT NATIVE**

**Justificación clave:**
1. **SignalR**: Excelente soporte con `@microsoft/signalr` (mismo paquete que web)
2. **WebRTC**: Madurez probada con `react-native-webrtc` (30k+ estrellas GitHub)
3. **Ecosistema**: Mayor cantidad de paquetes nativos para mensajería
4. **Experiencia del equipo**: Si ya tienen Angular/TypeScript, la curva de aprendizaje es menor
5. **Code Push**: Actualizaciones OTA maduras y estables
6. **Comunidad**: Más recursos específicos para apps de mensajería con SignalR

---

## 1. ANÁLISIS COMPARATIVO: REACT NATIVE vs FLUTTER

### 1.1 SignalR Integration

#### React Native ✅ VENTAJA SIGNIFICATIVA
```typescript
// Mismo paquete que web - compatibilidad garantizada
import * as signalR from '@microsoft/signalr';

const connection = new signalR.HubConnectionBuilder()
  .withUrl('https://api.example.com/hubs/chat', {
    accessTokenFactory: () => getToken()
  })
  .withAutomaticReconnect({
    nextRetryDelayInMilliseconds: (retryContext) => {
      // Estrategia de reconexión personalizada
      if (retryContext.elapsedMilliseconds < 60000) {
        return Math.random() * 10000;
      } else {
        return null; // Detener reintentos
      }
    }
  })
  .configureLogging(signalR.LogLevel.Information)
  .build();

// Manejo de estados de conexión
connection.onreconnecting(() => {
  store.dispatch(setConnectionStatus('reconnecting'));
});

connection.onreconnected(() => {
  store.dispatch(setConnectionStatus('connected'));
  // Re-unirse a grupos de chats
  reJoinAllChats();
});

connection.onclose(() => {
  store.dispatch(setConnectionStatus('disconnected'));
  // Reintentar conexión manual si necesario
});
```

**Ventajas:**
- Mismo paquete oficial de Microsoft
- Misma API que la web (reutilización de código)
- Soporte nativo para reconexión automática
- Binary MessagePack support para reducir payload
- Amplia documentación y ejemplos

**Manejo de reconexión en background (iOS/Android):**
```typescript
import { AppState } from 'react-native';

AppState.addEventListener('change', async (nextAppState) => {
  if (nextAppState === 'active') {
    // App volvió al foreground
    if (connection.state === signalR.HubConnectionState.Disconnected) {
      await connection.start();
      await reJoinAllChats();
    }
  } else if (nextAppState === 'background') {
    // App fue a background - mantener conexión para notificaciones
    // iOS: ~30 segundos de background execution
    // Android: más flexible con foreground service
  }
});
```

#### Flutter ⚠️ OPCIONES LIMITADAS
```dart
// signalr_netcore (paquete de comunidad, no oficial)
import 'package:signalr_netcore/signalr_client.dart';

final hubConnection = HubConnectionBuilder()
  .withUrl('https://api.example.com/hubs/chat',
    options: HttpConnectionOptions(
      accessTokenFactory: () async => await getToken(),
    ))
  .withAutomaticReconnect()
  .build();

// API similar pero paquete de terceros
await hubConnection.start();
```

**Desventajas:**
- Paquete de comunidad (signalr_netcore) - no oficial
- Menor frecuencia de actualizaciones
- Menos ejemplos y documentación específica
- Potenciales problemas de compatibilidad con nuevas versiones de SignalR

**Paquetes disponibles:**
- `signalr_netcore`: ~1.3k pub.dev score (community)
- `signalr_core`: Menos mantenido
- `flutter_signalr`: Descontinuado

---

### 1.2 WebRTC Integration

#### React Native ✅ EXCELENTE
```typescript
import {
  RTCPeerConnection,
  RTCSessionDescription,
  RTCIceCandidate,
  mediaDevices,
  MediaStream,
} from 'react-native-webrtc';

class WebRTCService {
  private peerConnection: RTCPeerConnection | null = null;
  private localStream: MediaStream | null = null;

  async initCall(isVideo: boolean) {
    // Obtener permisos y stream local
    const stream = await mediaDevices.getUserMedia({
      audio: true,
      video: isVideo ? {
        frameRate: 30,
        facingMode: 'user',
        width: 1280,
        height: 720,
      } : false,
    });

    this.localStream = stream;

    // Crear peer connection
    this.peerConnection = new RTCPeerConnection({
      iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
      ],
    });

    // Agregar tracks
    stream.getTracks().forEach(track => {
      this.peerConnection!.addTrack(track, stream);
    });

    // Manejar ICE candidates
    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        signalR.sendIceCandidate(otherUserId, JSON.stringify(event.candidate));
      }
    };

    // Recibir stream remoto
    this.peerConnection.ontrack = (event) => {
      this.remoteStream = event.streams[0];
      // Actualizar UI
    };

    return stream;
  }

  async createOffer() {
    const offer = await this.peerConnection!.createOffer();
    await this.peerConnection!.setLocalDescription(offer);
    return JSON.stringify(offer);
  }

  async handleAnswer(sdpAnswer: string) {
    const answer = JSON.parse(sdpAnswer);
    await this.peerConnection!.setRemoteDescription(
      new RTCSessionDescription(answer)
    );
  }

  toggleCamera() {
    const videoTrack = this.localStream?.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.enabled = !videoTrack.enabled;
    }
  }

  switchCamera() {
    // react-native-webrtc tiene método nativo
    const videoTrack = this.localStream?.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack._switchCamera();
    }
  }
}
```

**Paquete:** `react-native-webrtc`
- 30k+ estrellas GitHub
- Mantenimiento activo
- Soporte completo para iOS/Android
- CallKit (iOS) y ConnectionService (Android) integración

**Integración con CallKit (iOS):**
```typescript
import RNCallKit from 'react-native-callkeep';

RNCallKit.setup({
  ios: {
    appName: 'WhatsApp Clone',
  },
  android: {
    alertTitle: 'Permissions required',
    alertDescription: 'This app needs to access your phone accounts',
    cancelButton: 'Cancel',
    okButton: 'OK',
  }
});

// Mostrar llamada entrante en pantalla bloqueada
RNCallKit.displayIncomingCall(
  uuid,
  callerName,
  callerPhoto,
  'generic',
  true // hasVideo
);

// Manejar acciones del usuario
RNCallKit.addEventListener('answerCall', ({ callUUID }) => {
  // Responder llamada
  acceptCall(callUUID);
});

RNCallKit.addEventListener('endCall', ({ callUUID }) => {
  // Colgar
  endCall(callUUID);
});
```

#### Flutter ✅ BUENO (pero menos maduro)
```dart
import 'package:flutter_webrtc/flutter_webrtc.dart';

class WebRTCService {
  RTCPeerConnection? _peerConnection;
  MediaStream? _localStream;

  Future<void> initCall(bool isVideo) async {
    // Obtener stream
    _localStream = await navigator.mediaDevices.getUserMedia({
      'audio': true,
      'video': isVideo ? {
        'facingMode': 'user',
        'width': 1280,
        'height': 720,
      } : false,
    });

    // Crear peer connection
    _peerConnection = await createPeerConnection({
      'iceServers': [
        {'urls': 'stun:stun.l.google.com:19302'},
      ],
    });

    // Agregar tracks
    _localStream!.getTracks().forEach((track) {
      _peerConnection!.addTrack(track, _localStream!);
    });

    // ICE candidates
    _peerConnection!.onIceCandidate = (candidate) {
      if (candidate != null) {
        signalR.sendIceCandidate(otherUserId, jsonEncode(candidate.toMap()));
      }
    };

    // Remote stream
    _peerConnection!.onTrack = (event) {
      if (event.streams.isNotEmpty) {
        _remoteStream = event.streams[0];
        // Update UI
      }
    };
  }
}
```

**Paquete:** `flutter_webrtc`
- 4k+ estrellas GitHub
- Buen mantenimiento
- API similar a web

**CallKit/ConnectionService:**
- Requiere paquetes adicionales: `flutter_callkit_incoming`
- Integración más compleja
- Menos documentación

---

### 1.3 State Management

#### React Native
**Opción recomendada: Zustand**

```typescript
// stores/chatStore.ts
import create from 'zustand';
import { persist } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface Message {
  id: string;
  chatId: string;
  contenido: string;
  remitenteId: string;
  fechaEnvio: Date;
  estado: 'Enviado' | 'Entregado' | 'Leido';
}

interface Chat {
  id: string;
  nombre: string;
  ultimoMensaje?: Message;
  mensajesNoLeidos: number;
}

interface ChatStore {
  chats: Record<string, Chat>;
  messages: Record<string, Message[]>; // chatId -> messages
  selectedChatId: string | null;

  // Actions
  addMessage: (message: Message) => void;
  updateMessageStatus: (messageId: string, status: string) => void;
  setSelectedChat: (chatId: string) => void;
  markAsRead: (chatId: string) => void;
}

export const useChatStore = create<ChatStore>()(
  persist(
    (set, get) => ({
      chats: {},
      messages: {},
      selectedChatId: null,

      addMessage: (message) => set((state) => {
        const chatMessages = state.messages[message.chatId] || [];
        return {
          messages: {
            ...state.messages,
            [message.chatId]: [...chatMessages, message].sort(
              (a, b) => a.fechaEnvio.getTime() - b.fechaEnvio.getTime()
            ),
          },
          chats: {
            ...state.chats,
            [message.chatId]: {
              ...state.chats[message.chatId],
              ultimoMensaje: message,
            },
          },
        };
      }),

      updateMessageStatus: (messageId, status) => set((state) => {
        const updatedMessages = { ...state.messages };
        Object.keys(updatedMessages).forEach(chatId => {
          updatedMessages[chatId] = updatedMessages[chatId].map(msg =>
            msg.id === messageId ? { ...msg, estado: status } : msg
          );
        });
        return { messages: updatedMessages };
      }),

      setSelectedChat: (chatId) => set({ selectedChatId: chatId }),

      markAsRead: (chatId) => set((state) => ({
        chats: {
          ...state.chats,
          [chatId]: {
            ...state.chats[chatId],
            mensajesNoLeidos: 0,
          },
        },
      })),
    }),
    {
      name: 'chat-storage',
      storage: AsyncStorage,
      partialize: (state) => ({
        chats: state.chats,
        messages: state.messages,
      }),
    }
  )
);
```

**Alternativa: Redux Toolkit**
- Más boilerplate
- Mejor para apps muy grandes
- DevTools excelentes

#### Flutter
**Opción recomendada: Riverpod**

```dart
// providers/chat_provider.dart
import 'package:flutter_riverpod/flutter_riverpod.dart';

@freezed
class Message with _$Message {
  factory Message({
    required String id,
    required String chatId,
    required String contenido,
    required String remitenteId,
    required DateTime fechaEnvio,
    required String estado,
  }) = _Message;
}

class ChatNotifier extends StateNotifier<Map<String, List<Message>>> {
  ChatNotifier() : super({});

  void addMessage(Message message) {
    state = {
      ...state,
      message.chatId: [...(state[message.chatId] ?? []), message]
        ..sort((a, b) => a.fechaEnvio.compareTo(b.fechaEnvio)),
    };
  }

  void updateMessageStatus(String messageId, String status) {
    state = state.map((chatId, messages) => MapEntry(
      chatId,
      messages.map((msg) =>
        msg.id == messageId ? msg.copyWith(estado: status) : msg
      ).toList(),
    ));
  }
}

final chatProvider = StateNotifierProvider<ChatNotifier, Map<String, List<Message>>>(
  (ref) => ChatNotifier(),
);
```

**Alternativa: Bloc**
- Más boilerplate
- Pattern específico (Event-State)
- Curva de aprendizaje más alta

---

### 1.4 Offline Support & Local Database

#### React Native
**Opción recomendada: WatermelonDB**

```typescript
// models/Message.ts
import { Model, Q } from '@nozbe/watermelondb';
import { field, date, relation, readonly } from '@nozbe/watermelondb/decorators';

export class Message extends Model {
  static table = 'messages';
  static associations = {
    chats: { type: 'belongs_to', key: 'chat_id' },
  };

  @field('contenido') contenido!: string;
  @field('remitente_id') remitenteId!: string;
  @field('tipo') tipo!: string;
  @field('url_archivo') urlArchivo?: string;
  @field('estado') estado!: string;
  @date('fecha_envio') fechaEnvio!: Date;
  @field('is_synced') isSynced!: boolean;
  @readonly @date('created_at') createdAt!: Date;
  @readonly @date('updated_at') updatedAt!: Date;

  @relation('chats', 'chat_id') chat!: Relation<Chat>;
}

// database/sync.ts
export class SyncService {
  async syncMessages() {
    const unsyncedMessages = await database.collections
      .get<Message>('messages')
      .query(Q.where('is_synced', false))
      .fetch();

    for (const message of unsyncedMessages) {
      try {
        await api.sendMessage(message);
        await message.update(msg => {
          msg.isSynced = true;
        });
      } catch (error) {
        console.error('Sync failed:', error);
      }
    }
  }

  async saveIncomingMessage(messageDto: MessageDTO) {
    await database.write(async () => {
      await database.collections.get<Message>('messages').create(message => {
        message.contenido = messageDto.contenido;
        message.remitenteId = messageDto.remitenteId;
        message.estado = messageDto.estado;
        message.fechaEnvio = new Date(messageDto.fechaEnvio);
        message.isSynced = true;
      });
    });
  }
}
```

**Ventajas:**
- Optimizado para React Native
- Lazy loading automático
- Sincronización reactiva
- Performance excelente (SQLite nativo)

**Alternativas:**
- `realm`: Muy potente pero más pesado
- `react-native-sqlite-storage`: Más bajo nivel

#### Flutter
**Opción recomendada: Hive**

```dart
// models/message.dart
import 'package:hive/hive.dart';

part 'message.g.dart';

@HiveType(typeId: 0)
class Message extends HiveObject {
  @HiveField(0)
  String id;

  @HiveField(1)
  String chatId;

  @HiveField(2)
  String contenido;

  @HiveField(3)
  String remitenteId;

  @HiveField(4)
  DateTime fechaEnvio;

  @HiveField(5)
  String estado;

  @HiveField(6)
  bool isSynced;

  Message({
    required this.id,
    required this.chatId,
    required this.contenido,
    required this.remitenteId,
    required this.fechaEnvio,
    required this.estado,
    this.isSynced = false,
  });
}

// services/sync_service.dart
class SyncService {
  Future<void> syncMessages() async {
    final box = await Hive.openBox<Message>('messages');
    final unsyncedMessages = box.values.where((msg) => !msg.isSynced).toList();

    for (final message in unsyncedMessages) {
      try {
        await api.sendMessage(message);
        message.isSynced = true;
        await message.save();
      } catch (e) {
        print('Sync failed: $e');
      }
    }
  }
}
```

**Ventajas:**
- Muy rápido (NoSQL)
- API simple
- No requiere código nativo

**Alternativas:**
- `sqflite`: SQLite tradicional
- `isar`: Muy rápido, más moderno

---

## 2. ARQUITECTURA DETALLADA - REACT NATIVE

### 2.1 Estructura del Proyecto

```
whatsapp-clone-mobile/
├── android/                      # Configuración Android
├── ios/                          # Configuración iOS
├── src/
│   ├── api/
│   │   ├── axios.ts             # Instancia configurada de Axios
│   │   ├── endpoints.ts         # Endpoints REST
│   │   └── services/
│   │       ├── auth.service.ts
│   │       ├── chat.service.ts
│   │       ├── message.service.ts
│   │       ├── contact.service.ts
│   │       ├── status.service.ts
│   │       └── call.service.ts
│   ├── components/
│   │   ├── chat/
│   │   │   ├── ChatList.tsx
│   │   │   ├── ChatItem.tsx
│   │   │   ├── MessageBubble.tsx
│   │   │   ├── MessageInput.tsx
│   │   │   └── TypingIndicator.tsx
│   │   ├── call/
│   │   │   ├── IncomingCall.tsx
│   │   │   ├── ActiveCall.tsx
│   │   │   └── CallControls.tsx
│   │   ├── status/
│   │   │   ├── StatusList.tsx
│   │   │   ├── StatusViewer.tsx
│   │   │   └── CreateStatus.tsx
│   │   └── common/
│   │       ├── Avatar.tsx
│   │       ├── Button.tsx
│   │       └── LoadingSpinner.tsx
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   └── RegisterScreen.tsx
│   │   ├── chats/
│   │   │   ├── ChatsScreen.tsx
│   │   │   └── ChatDetailScreen.tsx
│   │   ├── status/
│   │   │   └── StatusScreen.tsx
│   │   ├── calls/
│   │   │   └── CallsScreen.tsx
│   │   └── settings/
│   │       ├── SettingsScreen.tsx
│   │       └── ProfileScreen.tsx
│   ├── services/
│   │   ├── signalr/
│   │   │   ├── SignalRService.ts
│   │   │   └── reconnection.ts
│   │   ├── webrtc/
│   │   │   ├── WebRTCService.ts
│   │   │   └── callkit.ts
│   │   ├── push/
│   │   │   └── PushNotificationService.ts
│   │   ├── offline/
│   │   │   ├── SyncService.ts
│   │   │   └── QueueService.ts
│   │   └── media/
│   │       ├── CameraService.ts
│   │       ├── AudioRecorderService.ts
│   │       └── MediaPickerService.ts
│   ├── store/
│   │   ├── slices/
│   │   │   ├── authSlice.ts
│   │   │   ├── chatSlice.ts
│   │   │   ├── messageSlice.ts
│   │   │   └── callSlice.ts
│   │   └── index.ts
│   ├── database/
│   │   ├── schema.ts
│   │   ├── models/
│   │   │   ├── Message.ts
│   │   │   ├── Chat.ts
│   │   │   └── User.ts
│   │   └── sync.ts
│   ├── navigation/
│   │   ├── AppNavigator.tsx
│   │   ├── AuthNavigator.tsx
│   │   └── MainNavigator.tsx
│   ├── hooks/
│   │   ├── useSignalR.ts
│   │   ├── useWebRTC.ts
│   │   ├── useMessages.ts
│   │   └── useMediaPermissions.ts
│   ├── utils/
│   │   ├── formatters.ts
│   │   ├── validators.ts
│   │   └── constants.ts
│   └── types/
│       ├── api.types.ts
│       ├── models.types.ts
│       └── navigation.types.ts
├── __tests__/
├── package.json
└── tsconfig.json
```

### 2.2 Navegación con React Navigation

```typescript
// navigation/MainNavigator.tsx
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

function ChatStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen
        name="ChatList"
        component={ChatsScreen}
        options={{ title: 'Chats' }}
      />
      <Stack.Screen
        name="ChatDetail"
        component={ChatDetailScreen}
        options={({ route }) => ({
          title: route.params.chatName,
          headerRight: () => (
            <View style={{ flexDirection: 'row' }}>
              <IconButton icon="video" onPress={() => startVideoCall()} />
              <IconButton icon="phone" onPress={() => startVoiceCall()} />
            </View>
          ),
        })}
      />
    </Stack.Navigator>
  );
}

export function MainNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ color, size }) => {
          const icons = {
            Chats: 'message-text',
            Status: 'progress-clock',
            Calls: 'phone',
          };
          return <Icon name={icons[route.name]} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#25D366',
        tabBarInactiveTintColor: 'gray',
      })}
    >
      <Tab.Screen name="Chats" component={ChatStack} />
      <Tab.Screen name="Status" component={StatusScreen} />
      <Tab.Screen name="Calls" component={CallsScreen} />
    </Tab.Navigator>
  );
}
```

**Deep linking:**
```typescript
// navigation/linking.ts
const linking = {
  prefixes: ['whatsappclone://', 'https://whatsappclone.com'],
  config: {
    screens: {
      Main: {
        screens: {
          Chats: {
            screens: {
              ChatDetail: 'chat/:chatId',
            },
          },
        },
      },
      IncomingCall: 'call/:callId',
    },
  },
};

// App.tsx
<NavigationContainer linking={linking}>
  <AppNavigator />
</NavigationContainer>
```

---

## 3. IMPLEMENTACIÓN DE FUNCIONALIDADES CRÍTICAS

### 3.1 Push Notifications con Firebase Cloud Messaging

```typescript
// services/push/PushNotificationService.ts
import messaging from '@react-native-firebase/messaging';
import notifee, { AndroidImportance } from '@notifee/react-native';

export class PushNotificationService {
  async initialize() {
    // Solicitar permisos
    const authStatus = await messaging().requestPermission();
    const enabled =
      authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
      authStatus === messaging.AuthorizationStatus.PROVISIONAL;

    if (!enabled) {
      console.log('Push notifications permission denied');
      return;
    }

    // Obtener FCM token
    const token = await messaging().getToken();
    await this.registerTokenWithBackend(token);

    // Escuchar mensajes en foreground
    messaging().onMessage(async (remoteMessage) => {
      await this.displayNotification(remoteMessage);
    });

    // Escuchar mensajes en background
    messaging().setBackgroundMessageHandler(async (remoteMessage) => {
      console.log('Message handled in background:', remoteMessage);
    });

    // Manejar notificación tocada
    notifee.onForegroundEvent(({ type, detail }) => {
      if (type === EventType.PRESS) {
        // Navegar al chat
        navigation.navigate('ChatDetail', { chatId: detail.notification.data.chatId });
      }
    });
  }

  async displayNotification(remoteMessage: FirebaseMessagingTypes.RemoteMessage) {
    // Crear canal de notificación (Android)
    const channelId = await notifee.createChannel({
      id: 'messages',
      name: 'Mensajes',
      importance: AndroidImportance.HIGH,
      sound: 'notification.mp3',
      vibration: true,
      vibrationPattern: [300, 500],
    });

    // Mostrar notificación
    await notifee.displayNotification({
      title: remoteMessage.notification?.title,
      body: remoteMessage.notification?.body,
      data: remoteMessage.data,
      android: {
        channelId,
        smallIcon: 'ic_notification',
        largeIcon: remoteMessage.data?.senderPhoto,
        importance: AndroidImportance.HIGH,
        pressAction: {
          id: 'default',
        },
        actions: [
          {
            title: 'Responder',
            pressAction: { id: 'reply' },
            input: {
              placeholder: 'Escribe tu respuesta...',
            },
          },
          {
            title: 'Marcar como leído',
            pressAction: { id: 'mark_read' },
          },
        ],
      },
      ios: {
        foregroundPresentationOptions: {
          alert: true,
          badge: true,
          sound: true,
        },
        categoryId: 'message',
      },
    });
  }

  async registerTokenWithBackend(token: string) {
    try {
      await api.post('/api/push/register', {
        token,
        platform: Platform.OS,
      });
    } catch (error) {
      console.error('Failed to register FCM token:', error);
    }
  }
}
```

**iOS: Configuración de notificaciones interactivas**
```typescript
// ios/NotificationService.swift (extension para rich notifications)
import UserNotifications

class NotificationService: UNNotificationServiceExtension {
    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        // Modificar contenido antes de mostrar
        let bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        // Descargar imagen del remitente
        if let imageUrlString = request.content.userInfo["senderPhoto"] as? String,
           let imageUrl = URL(string: imageUrlString) {
            downloadImage(from: imageUrl) { attachment in
                if let attachment = attachment {
                    bestAttemptContent?.attachments = [attachment]
                }
                contentHandler(bestAttemptContent!)
            }
        }
    }
}
```

### 3.2 Media Handling

```typescript
// services/media/CameraService.ts
import { launchCamera, launchImageLibrary } from 'react-native-image-picker';
import { check, request, PERMISSIONS, RESULTS } from 'react-native-permissions';

export class CameraService {
  async checkCameraPermission(): Promise<boolean> {
    const permission = Platform.select({
      ios: PERMISSIONS.IOS.CAMERA,
      android: PERMISSIONS.ANDROID.CAMERA,
    });

    const result = await check(permission!);

    if (result === RESULTS.DENIED) {
      const requestResult = await request(permission!);
      return requestResult === RESULTS.GRANTED;
    }

    return result === RESULTS.GRANTED;
  }

  async takePicture(): Promise<Asset | null> {
    const hasPermission = await this.checkCameraPermission();
    if (!hasPermission) return null;

    const result = await launchCamera({
      mediaType: 'photo',
      quality: 0.8,
      maxWidth: 1920,
      maxHeight: 1920,
      includeBase64: false,
    });

    if (result.didCancel || result.errorCode) {
      return null;
    }

    return result.assets?.[0] || null;
  }

  async recordVideo(): Promise<Asset | null> {
    const hasPermission = await this.checkCameraPermission();
    if (!hasPermission) return null;

    const result = await launchCamera({
      mediaType: 'video',
      videoQuality: 'high',
      durationLimit: 300, // 5 minutos
    });

    if (result.didCancel || result.errorCode) {
      return null;
    }

    return result.assets?.[0] || null;
  }

  async pickFromGallery(type: 'photo' | 'video'): Promise<Asset | null> {
    const result = await launchImageLibrary({
      mediaType: type,
      quality: 0.8,
      selectionLimit: 1,
    });

    if (result.didCancel || result.errorCode) {
      return null;
    }

    return result.assets?.[0] || null;
  }

  async uploadMedia(file: Asset, onProgress?: (progress: number) => void): Promise<string> {
    const formData = new FormData();
    formData.append('file', {
      uri: file.uri,
      type: file.type,
      name: file.fileName || 'upload.jpg',
    });

    const response = await api.post('/api/messages/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        const progress = progressEvent.loaded / progressEvent.total;
        onProgress?.(progress);
      },
    });

    return response.data.url;
  }
}
```

**Audio Recording:**
```typescript
// services/media/AudioRecorderService.ts
import AudioRecorderPlayer from 'react-native-audio-recorder-player';
import { check, request, PERMISSIONS } from 'react-native-permissions';

export class AudioRecorderService {
  private audioRecorderPlayer = new AudioRecorderPlayer();
  private recordingPath = '';

  async startRecording(): Promise<void> {
    const permission = Platform.select({
      ios: PERMISSIONS.IOS.MICROPHONE,
      android: PERMISSIONS.ANDROID.RECORD_AUDIO,
    });

    const result = await check(permission!);
    if (result !== RESULTS.GRANTED) {
      await request(permission!);
    }

    const path = Platform.select({
      ios: 'audio.m4a',
      android: `${RNFS.DocumentDirectoryPath}/audio.mp4`,
    });

    this.recordingPath = path!;

    await this.audioRecorderPlayer.startRecorder(path, {
      SampleRate: 44100,
      Channels: 1,
      AudioQuality: 'High',
      AudioEncoding: 'aac',
    });

    this.audioRecorderPlayer.addRecordBackListener((e) => {
      // Actualizar UI con duración
      const duration = Math.floor(e.currentPosition / 1000);
      // emit event
    });
  }

  async stopRecording(): Promise<{ uri: string; duration: number }> {
    const result = await this.audioRecorderPlayer.stopRecorder();
    this.audioRecorderPlayer.removeRecordBackListener();

    const info = await RNFS.stat(this.recordingPath);

    return {
      uri: this.recordingPath,
      duration: parseInt(result) / 1000,
    };
  }

  async playAudio(uri: string, onProgress?: (position: number, duration: number) => void) {
    await this.audioRecorderPlayer.startPlayer(uri);

    this.audioRecorderPlayer.addPlayBackListener((e) => {
      onProgress?.(e.currentPosition, e.duration);

      if (e.currentPosition === e.duration) {
        this.audioRecorderPlayer.stopPlayer();
      }
    });
  }

  async pauseAudio() {
    await this.audioRecorderPlayer.pausePlayer();
  }

  async stopAudio() {
    await this.audioRecorderPlayer.stopPlayer();
    this.audioRecorderPlayer.removePlayBackListener();
  }
}
```

### 3.3 Performance Optimization

```typescript
// components/chat/MessageList.tsx
import { FlashList } from '@shopify/flash-list';
import { memo } from 'react';

const MessageBubble = memo(({ message }: { message: Message }) => {
  const isOwnMessage = message.remitenteId === currentUserId;

  return (
    <View style={[styles.bubble, isOwnMessage && styles.ownBubble]}>
      {message.tipo === 'Texto' && (
        <Text style={styles.messageText}>{message.contenido}</Text>
      )}

      {message.tipo === 'Imagen' && (
        <FastImage
          source={{ uri: message.urlArchivo, priority: FastImage.priority.normal }}
          style={styles.image}
          resizeMode={FastImage.resizeMode.cover}
        />
      )}

      {message.tipo === 'Audio' && (
        <AudioPlayer uri={message.urlArchivo} duration={message.duracionSegundos} />
      )}

      <View style={styles.footer}>
        <Text style={styles.time}>
          {format(message.fechaEnvio, 'HH:mm')}
        </Text>
        {isOwnMessage && (
          <MessageStatusIcon status={message.estado} />
        )}
      </View>
    </View>
  );
}, (prevProps, nextProps) => {
  // Solo re-renderizar si el estado del mensaje cambió
  return prevProps.message.estado === nextProps.message.estado &&
         prevProps.message.id === nextProps.message.id;
});

export function MessageList({ chatId }: { chatId: string }) {
  const messages = useChatStore(state => state.messages[chatId] || []);

  return (
    <FlashList
      data={messages}
      renderItem={({ item }) => <MessageBubble message={item} />}
      estimatedItemSize={80}
      inverted // Mensajes más recientes abajo
      keyExtractor={(item) => item.id}
      // Optimización: mantener solo 50 mensajes renderizados
      maxToRenderPerBatch={20}
      windowSize={5}
      removeClippedSubviews={true}
    />
  );
}
```

**Image caching:**
```typescript
// Usar react-native-fast-image para cache automático
import FastImage from 'react-native-fast-image';

<FastImage
  source={{
    uri: imageUrl,
    priority: FastImage.priority.normal,
    cache: FastImage.cacheControl.immutable,
  }}
  style={{ width: 200, height: 200 }}
  resizeMode={FastImage.resizeMode.cover}
/>

// Pre-cargar imágenes
FastImage.preload([
  { uri: 'https://example.com/image1.jpg' },
  { uri: 'https://example.com/image2.jpg' },
]);
```

---

Continuaré con la siguiente sección...

---

## 4. COMPARACIÓN FLUTTER (ANÁLISIS COMPLETO)

### 4.1 Ventajas de Flutter

#### Performance Nativo
```dart
// Flutter compila a código nativo ARM
// Sin bridge JavaScript
// Rendering a 60/120 FPS consistente

class MessageList extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      itemCount: messages.length,
      itemBuilder: (context, index) {
        return MessageBubble(message: messages[index]);
      },
      // Rendering ultra-rápido con Skia
    );
  }
}
```

#### Hot Reload Superior
- **Stateful Hot Reload**: Mantiene estado de la app
- Más rápido que React Native Fast Refresh
- Menos bugs de hot reload

#### Un Solo Lenguaje
- Dart tanto para UI como lógica de negocio
- No hay bridge JS-Native
- Null safety built-in

#### Widgets Consistentes
```dart
// Same UI en iOS y Android
Material(
  child: Cupertino... // O usa Cupertino para look iOS
)
```

### 4.2 Desventajas de Flutter para ESTE Proyecto

#### 1. SignalR - Paquete de Comunidad
```dart
// signalr_netcore - no oficial de Microsoft
// Última actualización: 6 meses atrás
// Issues sin resolver
final hubConnection = HubConnectionBuilder()
  .withUrl(serverUrl)
  .build();

// Problemas reportados:
// - Reconexión inestable
// - MessagePack support limitado
// - Compatibilidad con nuevas versiones de SignalR
```

#### 2. Ecosistema de Paquetes Menor
```
React Native WebRTC: 30,000+ stars, mantenimiento activo
Flutter WebRTC: 4,000+ stars, menos documentación

React Native packages: ~20,000+
Flutter packages: ~35,000+ (pero menos maduros para messaging)
```

#### 3. Menor Comunidad para Apps de Mensajería
- Menos ejemplos de WhatsApp clones en Flutter
- Menos soluciones a problemas específicos de SignalR
- Documentación más limitada para WebRTC + SignalR

#### 4. Curva de Aprendizaje
Si el equipo ya conoce TypeScript/Angular:
- React Native: 1-2 semanas
- Flutter: 4-6 semanas (nuevo lenguaje Dart)

#### 5. Tamaño de App Mayor
```
React Native APK: ~20-30 MB
Flutter APK: ~15-20 MB (release mode)

Pero Flutter incluye engine Skia completo
Apps complejas: tamaños similares
```

### 4.3 Cuándo Elegir Flutter

**Flutter sería mejor SI:**
1. Backend NO usara SignalR (usara WebSockets nativos o gRPC)
2. Prioridad absoluta en performance de UI/animaciones
3. Equipo ya conoce Dart
4. No hay timeline urgente (más tiempo para aprender)
5. Se necesitan apps para Web, Desktop además de mobile

**Para ESTE proyecto específico: React Native es superior**

---

## 5. IMPLEMENTACIÓN DE CASOS DE USO CRÍTICOS

### 5.1 Flujo Completo: Enviar Mensaje con Archivo

```typescript
// screens/ChatDetailScreen.tsx
const sendImageMessage = async (imageAsset: Asset) => {
  const tempMessageId = uuid();
  const chatId = route.params.chatId;

  // 1. Crear mensaje temporal (optimistic update)
  const tempMessage: Message = {
    id: tempMessageId,
    chatId,
    contenido: '',
    tipo: TipoMensaje.Imagen,
    remitenteId: currentUser.id,
    fechaEnvio: new Date(),
    estado: EstadoMensaje.Enviado,
    urlArchivo: imageAsset.uri, // URI local
    isSynced: false,
    uploadProgress: 0,
  };

  // 2. Agregar a store inmediatamente (UX fluida)
  useChatStore.getState().addMessage(tempMessage);

  // 3. Guardar en base de datos local
  await database.write(async () => {
    await database.collections.get<Message>('messages').create(msg => {
      Object.assign(msg, tempMessage);
    });
  });

  try {
    // 4. Subir archivo al servidor
    const uploadedUrl = await mediaService.uploadMedia(
      imageAsset,
      (progress) => {
        // Actualizar progreso en UI
        useChatStore.getState().updateMessageProgress(tempMessageId, progress);
      }
    );

    // 5. Enviar mensaje con URL del servidor
    const response = await api.post('/api/messages', {
      chatId,
      contenido: '',
      tipo: 'Imagen',
      urlArchivo: uploadedUrl,
      nombreArchivo: imageAsset.fileName,
      tamanoArchivo: imageAsset.fileSize,
    });

    // 6. Actualizar mensaje con ID real del servidor
    useChatStore.getState().updateMessage(tempMessageId, {
      id: response.data.id,
      urlArchivo: uploadedUrl,
      isSynced: true,
    });

    // 7. Actualizar en base de datos
    await database.write(async () => {
      const msg = await database.collections.get<Message>('messages').find(tempMessageId);
      await msg.update(m => {
        m.id = response.data.id;
        m.urlArchivo = uploadedUrl;
        m.isSynced = true;
      });
    });

    // 8. SignalR notificará a otros participantes
    // (servidor envía ReceiveMessage a grupo del chat)

  } catch (error) {
    // Marcar mensaje como fallido
    useChatStore.getState().updateMessageStatus(tempMessageId, 'Failed');

    // Guardar en cola de reintentos
    await syncService.addToRetryQueue(tempMessage);

    // Mostrar error al usuario
    Toast.show({
      type: 'error',
      text1: 'Error al enviar imagen',
      text2: 'Se reintentará cuando haya conexión',
    });
  }
};
```

### 5.2 Flujo Completo: Recibir Llamada

```typescript
// services/call/CallManager.ts
export class CallManager {
  constructor(
    private signalr: SignalRService,
    private webrtc: WebRTCService,
    private navigation: NavigationContainerRef
  ) {
    this.setupCallListeners();
  }

  private setupCallListeners() {
    // 1. Escuchar oferta de llamada entrante
    this.signalr.onCallOffer(async (data) => {
      const { callerId, callerName, callerPhoto, callType, sdpOffer } = data;

      // 2. Mostrar pantalla de llamada entrante
      this.navigation.navigate('IncomingCall', {
        callerId,
        callerName,
        callerPhoto,
        callType,
      });

      // 3. Si iOS, mostrar con CallKit
      if (Platform.OS === 'ios') {
        const callUUID = uuid();
        RNCallKit.displayIncomingCall(
          callUUID,
          callerName,
          callerPhoto,
          'generic',
          callType === 'video'
        );

        // Escuchar respuesta desde CallKit
        RNCallKit.addEventListener('answerCall', async ({ callUUID }) => {
          await this.answerCall(callerId, sdpOffer, callType);
        });

        RNCallKit.addEventListener('endCall', async ({ callUUID }) => {
          await this.rejectCall(callerId);
        });
      }

      // 4. Si Android, usar ConnectionService
      if (Platform.OS === 'android') {
        RNCallKit.displayIncomingCall(
          uuid(),
          callerName,
          callerName,
          'number',
          callType === 'video'
        );
      }
    });

    // Escuchar respuesta a nuestra llamada
    this.signalr.onCallAnswer(async (data) => {
      await this.webrtc.handleAnswer(data.sdpAnswer);

      // Navegar a pantalla de llamada activa
      this.navigation.navigate('ActiveCall', {
        isVideo: this.currentCallType === 'video',
        otherUserId: this.otherUserId,
      });
    });

    // Llamada rechazada
    this.signalr.onCallRejected((data) => {
      this.webrtc.endCall();
      this.navigation.goBack();

      Toast.show({
        type: 'info',
        text1: 'Llamada rechazada',
      });
    });

    // Llamada terminada
    this.signalr.onCallEnded(() => {
      this.webrtc.endCall();
      this.navigation.goBack();
    });

    // ICE candidates
    this.signalr.onIceCandidate(async (data) => {
      await this.webrtc.addIceCandidate(data.candidate);
    });
  }

  async startCall(userId: string, callType: 'voice' | 'video') {
    try {
      // 1. Verificar permisos
      const permissions = await this.webrtc.checkPermissions();
      if (!permissions.audio || (callType === 'video' && !permissions.video)) {
        // Solicitar permisos
        await this.requestPermissions(callType);
      }

      // 2. Inicializar WebRTC
      await this.webrtc.initializeConnection((candidate) => {
        // Enviar ICE candidate al otro usuario
        this.signalr.sendIceCandidate(userId, candidate);
      });

      // 3. Crear oferta
      const sdpOffer = await this.webrtc.createOffer(callType === 'video');

      // 4. Enviar oferta vía SignalR
      await this.signalr.callUser(userId, callType, sdpOffer);

      // 5. Mostrar pantalla de llamada saliente
      this.navigation.navigate('OutgoingCall', {
        userId,
        callType,
      });

      this.otherUserId = userId;
      this.currentCallType = callType;

    } catch (error) {
      console.error('Error starting call:', error);
      Toast.show({
        type: 'error',
        text1: 'Error al iniciar llamada',
      });
    }
  }

  async answerCall(callerId: string, sdpOffer: string, callType: string) {
    try {
      // 1. Inicializar WebRTC
      await this.webrtc.initializeConnection((candidate) => {
        this.signalr.sendIceCandidate(callerId, candidate);
      });

      // 2. Crear respuesta
      const sdpAnswer = await this.webrtc.createAnswer(
        sdpOffer,
        callType === 'video'
      );

      // 3. Enviar respuesta vía SignalR
      await this.signalr.answerCall(callerId, sdpAnswer);

      // 4. Navegar a llamada activa
      this.navigation.navigate('ActiveCall', {
        isVideo: callType === 'video',
        otherUserId: callerId,
      });

      this.otherUserId = callerId;
      this.currentCallType = callType;

    } catch (error) {
      console.error('Error answering call:', error);
      await this.rejectCall(callerId);
    }
  }

  async rejectCall(callerId: string) {
    await this.signalr.rejectCall(callerId, 'Rejected');
    this.navigation.goBack();
  }

  async endCall() {
    if (this.otherUserId) {
      await this.signalr.endCall(this.otherUserId);
    }
    this.webrtc.endCall();
    this.navigation.goBack();
  }
}
```

### 5.3 Sincronización Offline Inteligente

```typescript
// services/offline/SyncService.ts
import NetInfo from '@react-native-community/netinfo';
import { database } from '../database';
import { Q } from '@nozbe/watermelondb';

export class SyncService {
  private syncInProgress = false;
  private retryQueue: Message[] = [];

  constructor() {
    this.setupNetworkListener();
  }

  private setupNetworkListener() {
    NetInfo.addEventListener(state => {
      if (state.isConnected && !this.syncInProgress) {
        this.syncAll();
      }
    });
  }

  async syncAll() {
    if (this.syncInProgress) return;

    this.syncInProgress = true;

    try {
      // 1. Sincronizar mensajes salientes
      await this.syncOutgoingMessages();

      // 2. Sincronizar estados de mensajes
      await this.syncMessageStatuses();

      // 3. Descargar mensajes nuevos
      await this.fetchNewMessages();

      // 4. Sincronizar llamadas perdidas
      await this.syncMissedCalls();

      // 5. Sincronizar estados/stories
      await this.syncStatuses();

    } catch (error) {
      console.error('Sync failed:', error);
      // Reintentar en 30 segundos
      setTimeout(() => this.syncAll(), 30000);
    } finally {
      this.syncInProgress = false;
    }
  }

  private async syncOutgoingMessages() {
    // Obtener mensajes no sincronizados
    const unsyncedMessages = await database.collections
      .get<Message>('messages')
      .query(Q.where('is_synced', false))
      .fetch();

    for (const message of unsyncedMessages) {
      try {
        // Si tiene archivo, subirlo primero
        if (message.urlArchivo && message.urlArchivo.startsWith('file://')) {
          const uploadedUrl = await this.uploadFile(message.urlArchivo);
          message.urlArchivo = uploadedUrl;
        }

        // Enviar mensaje al servidor
        const response = await api.post('/api/messages', {
          chatId: message.chatId,
          contenido: message.contenido,
          tipo: message.tipo,
          urlArchivo: message.urlArchivo,
          nombreArchivo: message.nombreArchivo,
          tamanoArchivo: message.tamanoArchivo,
        });

        // Actualizar mensaje con ID del servidor
        await database.write(async () => {
          await message.update(m => {
            m.serverId = response.data.id;
            m.isSynced = true;
            m.fechaEnvio = new Date(response.data.fechaEnvio);
          });
        });

        // Actualizar en store
        useChatStore.getState().updateMessage(message.id, {
          serverId: response.data.id,
          isSynced: true,
        });

      } catch (error) {
        console.error(`Failed to sync message ${message.id}:`, error);

        // Si es error 4xx, marcar como failed permanentemente
        if (error.response?.status >= 400 && error.response?.status < 500) {
          await database.write(async () => {
            await message.update(m => {
              m.failed = true;
            });
          });
        }
        // Si es 5xx o red, se reintentará en próximo sync
      }
    }
  }

  private async fetchNewMessages() {
    // Obtener timestamp del último mensaje
    const lastMessage = await database.collections
      .get<Message>('messages')
      .query(Q.sortBy('fecha_envio', Q.desc), Q.take(1))
      .fetch();

    const lastTimestamp = lastMessage[0]?.fechaEnvio || new Date(0);

    // Obtener mensajes nuevos del servidor
    const response = await api.get('/api/messages/new', {
      params: { since: lastTimestamp.toISOString() },
    });

    // Guardar en base de datos
    await database.write(async () => {
      for (const messageDto of response.data) {
        await database.collections.get<Message>('messages').create(msg => {
          msg.serverId = messageDto.id;
          msg.chatId = messageDto.chatId;
          msg.contenido = messageDto.contenido;
          msg.tipo = messageDto.tipo;
          msg.remitenteId = messageDto.remitenteId;
          msg.fechaEnvio = new Date(messageDto.fechaEnvio);
          msg.estado = messageDto.estado;
          msg.urlArchivo = messageDto.urlArchivo;
          msg.isSynced = true;
        });
      }
    });

    // Actualizar store
    response.data.forEach(msg => {
      useChatStore.getState().addMessage(msg);
    });
  }

  async addToRetryQueue(message: Message) {
    this.retryQueue.push(message);

    // Intentar enviar después de 5 segundos
    setTimeout(async () => {
      const index = this.retryQueue.indexOf(message);
      if (index > -1) {
        this.retryQueue.splice(index, 1);
        await this.syncOutgoingMessages();
      }
    }, 5000);
  }
}
```

---

## 6. TESTING STRATEGY

### 6.1 Unit Tests (Jest)

```typescript
// __tests__/services/SignalRService.test.ts
import { SignalRService } from '../../src/services/signalr/SignalRService';
import * as signalR from '@microsoft/signalr';

jest.mock('@microsoft/signalr');

describe('SignalRService', () => {
  let service: SignalRService;
  let mockConnection: any;

  beforeEach(() => {
    mockConnection = {
      start: jest.fn().mockResolvedValue(undefined),
      stop: jest.fn().mockResolvedValue(undefined),
      on: jest.fn(),
      invoke: jest.fn().mockResolvedValue(undefined),
      onreconnecting: jest.fn(),
      onreconnected: jest.fn(),
      state: signalR.HubConnectionState.Connected,
    };

    (signalR.HubConnectionBuilder as jest.Mock).mockReturnValue({
      withUrl: jest.fn().mockReturnThis(),
      withAutomaticReconnect: jest.fn().mockReturnThis(),
      build: jest.fn().mockReturnValue(mockConnection),
    });

    service = new SignalRService();
  });

  describe('startConnection', () => {
    it('should establish connection successfully', async () => {
      await service.startConnection('fake-token');

      expect(mockConnection.start).toHaveBeenCalled();
      expect(service.isConnected()).toBe(true);
    });

    it('should register message handlers', async () => {
      await service.startConnection('fake-token');

      expect(mockConnection.on).toHaveBeenCalledWith('ReceiveMessage', expect.any(Function));
      expect(mockConnection.on).toHaveBeenCalledWith('UserTyping', expect.any(Function));
    });

    it('should handle connection failure', async () => {
      mockConnection.start.mockRejectedValue(new Error('Connection failed'));

      await service.startConnection('fake-token');

      expect(service.isConnected()).toBe(false);
    });
  });

  describe('sendMessage', () => {
    it('should invoke SendMessage on hub', async () => {
      await service.startConnection('fake-token');

      const messageDto = {
        chatId: 'chat-1',
        mensaje: { contenido: 'Hello' },
      };

      await service.sendMessage(messageDto);

      expect(mockConnection.invoke).toHaveBeenCalledWith('SendMessage', messageDto);
    });
  });
});
```

### 6.2 Component Tests (React Native Testing Library)

```typescript
// __tests__/components/MessageBubble.test.tsx
import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { MessageBubble } from '../../src/components/chat/MessageBubble';

describe('MessageBubble', () => {
  const mockMessage = {
    id: 'msg-1',
    contenido: 'Test message',
    tipo: TipoMensaje.Texto,
    remitenteId: 'user-1',
    fechaEnvio: new Date('2024-01-01T10:00:00Z'),
    estado: EstadoMensaje.Leido,
  };

  it('should render text message correctly', () => {
    const { getByText } = render(
      <MessageBubble message={mockMessage} isOwnMessage={false} />
    );

    expect(getByText('Test message')).toBeTruthy();
    expect(getByText('10:00')).toBeTruthy();
  });

  it('should show read status for own messages', () => {
    const { getByTestId } = render(
      <MessageBubble message={mockMessage} isOwnMessage={true} />
    );

    const statusIcon = getByTestId('message-status-icon');
    expect(statusIcon.props.name).toBe('check-all'); // Double check
  });

  it('should call onLongPress when bubble is held', () => {
    const onLongPress = jest.fn();
    const { getByTestId } = render(
      <MessageBubble
        message={mockMessage}
        isOwnMessage={false}
        onLongPress={onLongPress}
      />
    );

    const bubble = getByTestId('message-bubble');
    fireEvent(bubble, 'onLongPress');

    expect(onLongPress).toHaveBeenCalledWith(mockMessage);
  });

  it('should render image message with thumbnail', () => {
    const imageMessage = {
      ...mockMessage,
      tipo: TipoMensaje.Imagen,
      urlArchivo: 'https://example.com/image.jpg',
    };

    const { getByTestId } = render(
      <MessageBubble message={imageMessage} isOwnMessage={false} />
    );

    const image = getByTestId('message-image');
    expect(image.props.source.uri).toBe('https://example.com/image.jpg');
  });
});
```

### 6.3 Integration Tests

```typescript
// __tests__/integration/chat-flow.test.ts
import { render, waitFor, fireEvent } from '@testing-library/react-native';
import { NavigationContainer } from '@react-navigation/native';
import { ChatDetailScreen } from '../../src/screens/chats/ChatDetailScreen';
import { SignalRService } from '../../src/services/signalr/SignalRService';
import { useChatStore } from '../../src/store';

jest.mock('../../src/services/signalr/SignalRService');

describe('Chat Flow Integration', () => {
  let mockSignalR: jest.Mocked<SignalRService>;

  beforeEach(() => {
    mockSignalR = new SignalRService() as jest.Mocked<SignalRService>;
    useChatStore.setState({ chats: {}, messages: {} });
  });

  it('should send message and receive confirmation', async () => {
    const { getByTestId, getByText } = render(
      <NavigationContainer>
        <ChatDetailScreen route={{ params: { chatId: 'chat-1' } }} />
      </NavigationContainer>
    );

    // Escribir mensaje
    const input = getByTestId('message-input');
    fireEvent.changeText(input, 'Hello world');

    // Enviar
    const sendButton = getByTestId('send-button');
    fireEvent.press(sendButton);

    // Mensaje debe aparecer en la lista
    await waitFor(() => {
      expect(getByText('Hello world')).toBeTruthy();
    });

    // Debe llamar a SignalR
    expect(mockSignalR.sendMessage).toHaveBeenCalled();

    // Simular confirmación del servidor
    const messageDto = {
      chatId: 'chat-1',
      mensaje: {
        id: 'msg-1',
        contenido: 'Hello world',
        estado: EstadoMensaje.Entregado,
      },
    };

    // Trigger message received event
    mockSignalR.messageReceived$.next(messageDto);

    // Estado debe actualizarse a Entregado
    await waitFor(() => {
      const message = useChatStore.getState().messages['chat-1'][0];
      expect(message.estado).toBe(EstadoMensaje.Entregado);
    });
  });
});
```

### 6.4 E2E Tests (Detox)

```typescript
// e2e/chat.e2e.ts
describe('Chat E2E', () => {
  beforeAll(async () => {
    await device.launchApp();
  });

  beforeEach(async () => {
    await device.reloadReactNative();
  });

  it('should login and send message', async () => {
    // Login
    await element(by.id('email-input')).typeText('test@example.com');
    await element(by.id('password-input')).typeText('password123');
    await element(by.id('login-button')).tap();

    // Esperar a que cargue lista de chats
    await waitFor(element(by.id('chat-list')))
      .toBeVisible()
      .withTimeout(5000);

    // Abrir primer chat
    await element(by.id('chat-item-0')).tap();

    // Esperar a que cargue el chat
    await waitFor(element(by.id('message-input')))
      .toBeVisible()
      .withTimeout(2000);

    // Escribir y enviar mensaje
    await element(by.id('message-input')).typeText('E2E test message');
    await element(by.id('send-button')).tap();

    // Verificar que el mensaje aparece
    await waitFor(element(by.text('E2E test message')))
      .toBeVisible()
      .withTimeout(3000);
  });

  it('should make voice call', async () => {
    // Navegar a chat
    await element(by.id('chat-item-0')).tap();

    // Presionar botón de llamada
    await element(by.id('voice-call-button')).tap();

    // Verificar que se muestra pantalla de llamada
    await waitFor(element(by.id('calling-screen')))
      .toBeVisible()
      .withTimeout(2000);

    // Verificar que se muestra nombre del contacto
    await expect(element(by.id('caller-name'))).toBeVisible();

    // Colgar
    await element(by.id('end-call-button')).tap();

    // Debe volver al chat
    await waitFor(element(by.id('message-input')))
      .toBeVisible()
      .withTimeout(2000);
  });
});
```

---

## 7. DEPLOYMENT Y CI/CD

### 7.1 GitHub Actions Workflow

```yaml
# .github/workflows/mobile-ci.yml
name: Mobile CI/CD

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Run linter
        run: npm run lint

      - name: Run tests
        run: npm test -- --coverage

      - name: Upload coverage
        uses: codecov/codecov-action@v3

  build-android:
    runs-on: ubuntu-latest
    needs: test
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '11'

      - name: Install dependencies
        run: npm ci

      - name: Decode Keystore
        run: |
          echo "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 -d > android/app/release.keystore

      - name: Build Android Release
        run: |
          cd android
          ./gradlew bundleRelease
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

      - name: Upload AAB
        uses: actions/upload-artifact@v3
        with:
          name: app-release.aab
          path: android/app/build/outputs/bundle/release/app-release.aab

      - name: Deploy to Play Store (Beta)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_STORE_JSON_KEY }}
          packageName: com.whatsappclone
          releaseFiles: android/app/build/outputs/bundle/release/app-release.aab
          track: beta

  build-ios:
    runs-on: macos-latest
    needs: test
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: npm ci

      - name: Install CocoaPods
        run: cd ios && pod install

      - name: Build iOS
        run: |
          xcodebuild -workspace ios/WhatsAppClone.xcworkspace \
            -scheme WhatsAppClone \
            -sdk iphoneos \
            -configuration Release \
            archive -archivePath $PWD/build/WhatsAppClone.xcarchive

      - name: Export IPA
        run: |
          xcodebuild -exportArchive \
            -archivePath $PWD/build/WhatsAppClone.xcarchive \
            -exportOptionsPlist ios/ExportOptions.plist \
            -exportPath $PWD/build

      - name: Upload to TestFlight
        uses: apple-actions/upload-testflight-build@v1
        with:
          app-path: build/WhatsAppClone.ipa
          issuer-id: ${{ secrets.APPSTORE_ISSUER_ID }}
          api-key-id: ${{ secrets.APPSTORE_API_KEY_ID }}
          api-private-key: ${{ secrets.APPSTORE_API_PRIVATE_KEY }}
```

### 7.2 CodePush Deployment

```typescript
// App.tsx
import codePush from 'react-native-code-push';

const codePushOptions = {
  checkFrequency: codePush.CheckFrequency.ON_APP_RESUME,
  installMode: codePush.InstallMode.ON_NEXT_RESUME,
  mandatoryInstallMode: codePush.InstallMode.IMMEDIATE,
};

function App() {
  // App code...
}

export default codePush(codePushOptions)(App);
```

```bash
# Deployment script
# appcenter-deploy.sh

#!/bin/bash

# Deploy to Staging
appcenter codepush release-react \
  -a MyOrg/WhatsAppClone-Android \
  -d Staging \
  -m --description "Bug fixes and performance improvements"

# Después de testing, promover a Production
appcenter codepush promote \
  -a MyOrg/WhatsAppClone-Android \
  -s Staging \
  -d Production
```

---

## 8. CONCLUSIÓN Y RECOMENDACIONES

### Recomendación Final: **REACT NATIVE**

#### Puntos Clave de la Decisión

1. **SignalR**: Paquete oficial de Microsoft, mismo que web
2. **WebRTC**: Ecosistema más maduro (react-native-webrtc)
3. **Curva de aprendizaje**: Menor para equipos TypeScript
4. **Comunidad**: Más recursos para apps de mensajería
5. **Time to Market**: 3.5-4 meses vs 5-6 meses con Flutter

#### Stack Tecnológico Recomendado

```
Frontend Mobile:
├── React Native 0.73+
├── TypeScript 5+
├── React Navigation 6
├── Zustand (state management)
├── WatermelonDB (offline database)
├── @microsoft/signalr
├── react-native-webrtc
├── @react-native-firebase/messaging
├── @notifee/react-native
├── react-native-callkeep
└── FlashList (performance)

Backend (existente):
├── ASP.NET Core
├── SignalR
├── MySQL
└── JWT Auth
```

#### Timeline Estimado

| Fase | Duración | Hitos |
|------|----------|-------|
| Setup y Auth | 2 semanas | Login funcional |
| SignalR + Chat básico | 2 semanas | Enviar/recibir mensajes |
| Multimedia | 2 semanas | Fotos, videos, audio |
| WebRTC Calls | 2 semanas | Llamadas de voz/video |
| Status/Stories | 1 semana | Estados con timer |
| Grupos + Contactos | 1 semana | Gestión completa |
| Push Notifications | 1 semana | FCM integrado |
| Offline Sync | 1 semana | Sincronización robusta |
| Testing | 2 semanas | Unit, Integration, E2E |
| Optimización | 1 semana | Performance, bundle size |
| Release Prep | 1 semana | Stores, docs |
| **TOTAL** | **16 semanas** | **~4 meses** |

#### Equipo Requerido

- 2x Desarrolladores React Native (Senior)
- 1x Backend Developer (ajustes API si necesario)
- 1x QA Engineer
- 1x UI/UX Designer (part-time)

#### Costos Estimados

```
Desarrollo: 2 devs × 4 meses × $8,000/mes = $64,000
Backend: 1 dev × 2 meses × $7,000/mes = $14,000
QA: 1 tester × 3 meses × $5,000/mes = $15,000
Design: 1 designer × 2 meses × $4,000/mes = $8,000

Infraestructura:
- Apple Developer: $99/año
- Google Play: $25 (one-time)
- Firebase Blaze: ~$50-200/mes
- AppCenter (CodePush): Gratis para <3 apps
- Sentry: $26/mes (Team plan)

TOTAL: ~$101,000 + $500/mes operacional
```

#### Riesgos Principales

1. **WebRTC Complexity** (Alta probabilidad, Alto impacto)
   - **Mitigación**: Iniciar early, tener plan B (Twilio)

2. **Performance en Android Low-End** (Media probabilidad, Medio impacto)
   - **Mitigación**: Testing en dispositivos gama baja desde Sprint 1

3. **Push Notifications Reliability** (Media probabilidad, Alto impacto)
   - **Mitigación**: Implementar fallback polling, testing exhaustivo

4. **Scope Creep** (Alta probabilidad, Alto impacto)
   - **Mitigación**: MVP estricto, features adicionales en v2

#### Próximos Pasos Inmediatos

1. ✅ **Aprobar este plan** con stakeholders
2. ✅ **Setup de repositorio** y Git workflow
3. ✅ **Onboarding del equipo** (1 semana)
4. ✅ **Sprint 0**: Setup de proyecto, CI/CD, Firebase (1 semana)
5. ✅ **Sprint 1**: Autenticación y navegación básica (2 semanas)

---

## ANEXOS

### A. Dependencias Completas

```json
{
  "dependencies": {
    "react": "18.2.0",
    "react-native": "0.73.2",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/native-stack": "^6.9.17",
    "@react-navigation/bottom-tabs": "^6.5.11",
    "zustand": "^4.4.7",
    "@microsoft/signalr": "^8.0.0",
    "react-native-webrtc": "^118.0.0",
    "@react-native-firebase/app": "^19.0.0",
    "@react-native-firebase/messaging": "^19.0.0",
    "@notifee/react-native": "^7.8.2",
    "react-native-callkeep": "^4.3.12",
    "@nozbe/watermelondb": "^0.27.1",
    "@shopify/flash-list": "^1.6.3",
    "react-native-fast-image": "^8.6.3",
    "react-native-image-picker": "^7.1.0",
    "react-native-audio-recorder-player": "^3.6.3",
    "react-native-video": "^5.2.1",
    "react-native-document-picker": "^9.1.1",
    "react-native-permissions": "^4.0.3",
    "react-native-keychain": "^8.1.2",
    "@react-native-async-storage/async-storage": "^1.21.0",
    "@react-native-community/netinfo": "^11.2.1",
    "axios": "^1.6.5",
    "date-fns": "^3.0.6",
    "react-native-toast-message": "^2.2.0",
    "react-native-gesture-handler": "^2.14.1",
    "react-native-reanimated": "^3.6.1"
  },
  "devDependencies": {
    "@types/react": "^18.2.45",
    "@types/react-native": "^0.73.0",
    "typescript": "^5.3.3",
    "@testing-library/react-native": "^12.4.3",
    "jest": "^29.7.0",
    "detox": "^20.17.0",
    "@typescript-eslint/eslint-plugin": "^6.17.0",
    "eslint": "^8.56.0",
    "prettier": "^3.1.1"
  }
}
```

### B. Alternativa: Si Eligen Flutter

Si por alguna razón el equipo decide ir con Flutter, la estructura sería:

```dart
// Estructura de proyecto Flutter
lib/
├── main.dart
├── app/
│   ├── routes.dart
│   └── theme.dart
├── core/
│   ├── api/
│   ├── services/
│   │   ├── signalr_service.dart  // signalr_netcore
│   │   ├── webrtc_service.dart   // flutter_webrtc
│   │   └── storage_service.dart  // hive
│   └── models/
├── features/
│   ├── auth/
│   ├── chat/
│   ├── calls/
│   └── status/
└── widgets/

// State management con Riverpod
final chatProvider = StateNotifierProvider<ChatNotifier, ChatState>((ref) {
  return ChatNotifier();
});
```

**Timeline con Flutter: 5-6 meses** (20-24 semanas)
- +4-8 semanas por curva de aprendizaje Dart
- +2-3 semanas por debugging de SignalR community package

---

**Documento preparado por:** Equipo de Arquitectura
**Fecha:** 2026-02-14
**Versión:** 1.0
**Estado:** Pendiente de Aprobación

