# Lista de Tareas - Desarrollo Mobile App

**Proyecto:** WhatsApp Clone - Aplicación Móvil React Native
**Fecha inicio:** 2026-02-14

---

## FASE 1: CONFIGURACIÓN Y FUNDAMENTOS (Semana 1-2)

### 1.1 Setup Inicial
- [ ] Crear proyecto React Native con TypeScript
- [ ] Configurar estructura de carpetas
- [ ] Instalar dependencias principales (React Navigation, Zustand, etc.)
- [ ] Configurar ESLint y Prettier
- [ ] Configurar ambiente de desarrollo (Android Studio, Xcode)
- [ ] Setup CI/CD básico (GitHub Actions o Bitrise)

### 1.2 Configuración de Firebase
- [ ] Crear proyecto en Firebase Console
- [ ] Configurar Firebase para Android (google-services.json)
- [ ] Configurar Firebase para iOS (GoogleService-Info.plist)
- [ ] Instalar @react-native-firebase/app
- [ ] Instalar @react-native-firebase/messaging (FCM)
- [ ] Probar notificaciones push básicas

### 1.3 Configuración de Base de Datos Local
- [ ] Instalar WatermelonDB
- [ ] Definir esquemas (Message, Chat, User, Contact)
- [ ] Configurar migraciones
- [ ] Crear modelos y relaciones
- [ ] Implementar servicio de sincronización básico

---

## FASE 2: AUTENTICACIÓN Y NAVEGACIÓN (Semana 2-3)

### 2.1 Sistema de Autenticación
- [ ] Crear pantallas de Login y Registro
- [ ] Implementar servicio de autenticación (JWT)
- [ ] Configurar almacenamiento seguro de tokens (react-native-keychain)
- [ ] Implementar interceptor de Axios para JWT
- [ ] Crear AuthContext/Store
- [ ] Implementar flujo de refresh token
- [ ] Manejo de sesiones expiradas

### 2.2 Navegación
- [ ] Configurar React Navigation (Stack, Tabs, Drawer)
- [ ] Crear navegación principal (Chats, Status, Calls)
- [ ] Implementar deep linking
- [ ] Configurar navegación condicional (auth vs main)
- [ ] Agregar transiciones personalizadas

---

## FASE 3: SIGNALR Y TIEMPO REAL (Semana 3-4)

### 3.1 Integración SignalR
- [ ] Instalar @microsoft/signalr
- [ ] Crear SignalRService
- [ ] Implementar conexión con autenticación JWT
- [ ] Configurar reconexión automática
- [ ] Implementar manejo de estados de conexión
- [ ] Integrar con AppState (background/foreground)

### 3.2 Eventos en Tiempo Real
- [ ] Implementar recepción de mensajes (ReceiveMessage)
- [ ] Implementar indicador de escritura (UserTyping)
- [ ] Implementar actualización de estado de mensajes
- [ ] Implementar estado online/offline de usuarios
- [ ] Implementar eventos de grupos (participantes, etc.)
- [ ] Crear sistema de listeners reactivos

---

## FASE 4: CHAT Y MENSAJERÍA (Semana 4-6)

### 4.1 Lista de Chats
- [ ] Crear componente ChatList con FlashList
- [ ] Implementar ChatItem con preview de último mensaje
- [ ] Agregar badges de mensajes no leídos
- [ ] Implementar búsqueda de chats
- [ ] Agregar swipe actions (archivar, silenciar, eliminar)
- [ ] Implementar ordenamiento por último mensaje
- [ ] Agregar estados visuales (escribiendo, en línea)

### 4.2 Ventana de Chat
- [ ] Crear componente MessageList (FlashList invertido)
- [ ] Implementar MessageBubble con diferentes tipos
- [ ] Crear MessageInput con emoji picker
- [ ] Implementar envío de mensajes de texto
- [ ] Agregar indicador de carga mientras envía
- [ ] Implementar scroll automático a nuevo mensaje
- [ ] Agregar botón "scroll to bottom"
- [ ] Implementar carga de mensajes paginada (infinite scroll)

### 4.3 Estados de Mensajes
- [ ] Implementar visualización de estados (Enviado, Entregado, Leído)
- [ ] Actualizar estados en tiempo real vía SignalR
- [ ] Persistir estados en base de datos local
- [ ] Implementar doble check (azul cuando leído)
- [ ] Agregar timestamp en mensajes

### 4.4 Mensajes Multimedia
- [ ] Implementar envío de imágenes (cámara + galería)
- [ ] Implementar preview de imagen antes de enviar
- [ ] Agregar compresión de imágenes
- [ ] Implementar envío de videos
- [ ] Agregar thumbnail para videos
- [ ] Implementar reproductor de video en chat
- [ ] Crear visor de imágenes fullscreen con zoom

### 4.5 Mensajes de Audio
- [ ] Implementar grabación de audio (hold to record)
- [ ] Crear componente de waveform visual
- [ ] Implementar cancelación de grabación (slide to cancel)
- [ ] Agregar reproductor de audio con waveform
- [ ] Implementar control de velocidad de reproducción
- [ ] Mostrar duración del audio

### 4.6 Documentos y Ubicación
- [ ] Implementar envío de documentos (PDF, etc.)
- [ ] Agregar preview de documentos
- [ ] Implementar compartir ubicación
- [ ] Mostrar mapa en mensajes de ubicación
- [ ] Implementar ubicación en tiempo real

---

## FASE 5: WEBRTC Y LLAMADAS (Semana 6-8)

### 5.1 Configuración WebRTC
- [ ] Instalar react-native-webrtc
- [ ] Crear WebRTCService
- [ ] Implementar gestión de peer connections
- [ ] Configurar STUN servers
- [ ] Implementar manejo de ICE candidates
- [ ] Integrar con SignalR para señalización

### 5.2 Llamadas de Voz
- [ ] Crear pantalla de llamada saliente
- [ ] Crear pantalla de llamada entrante
- [ ] Implementar lógica de iniciar llamada
- [ ] Implementar lógica de responder llamada
- [ ] Implementar lógica de rechazar llamada
- [ ] Agregar controles (mute, speaker)
- [ ] Mostrar duración de llamada
- [ ] Implementar finalización de llamada

### 5.3 Videollamadas
- [ ] Implementar captura de video local
- [ ] Mostrar video remoto
- [ ] Agregar controles de video (cámara on/off, flip)
- [ ] Implementar picture-in-picture
- [ ] Optimizar calidad según conexión
- [ ] Agregar efectos de video (blur background, etc.)

### 5.4 CallKit (iOS) y ConnectionService (Android)
- [ ] Instalar react-native-callkeep
- [ ] Configurar CallKit para iOS
- [ ] Configurar ConnectionService para Android
- [ ] Mostrar llamadas en pantalla bloqueada
- [ ] Integrar con sistema de llamadas nativo
- [ ] Manejar interrupciones (llamada telefónica regular)

### 5.5 Historial de Llamadas
- [ ] Crear pantalla de historial
- [ ] Guardar llamadas en base de datos
- [ ] Mostrar tipo (voz/video), duración, estado
- [ ] Implementar llamar desde historial
- [ ] Agregar filtros (todas, perdidas)

---

## FASE 6: ESTADOS/STORIES (Semana 8-9)

### 6.1 Ver Estados
- [ ] Crear pantalla de lista de estados
- [ ] Implementar círculos de estados con progreso
- [ ] Crear visor de estados (fullscreen)
- [ ] Agregar gestos (tap para avanzar, hold para pausar)
- [ ] Implementar timer de 24h
- [ ] Mostrar vistas de estados propios

### 6.2 Crear Estados
- [ ] Implementar cámara para estados
- [ ] Agregar filtros y stickers
- [ ] Implementar texto sobre imagen/video
- [ ] Agregar preview antes de publicar
- [ ] Implementar carga con progreso
- [ ] Notificar vía SignalR a contactos

---

## FASE 7: CONTACTOS Y GRUPOS (Semana 9-10)

### 7.1 Gestión de Contactos
- [ ] Crear pantalla de contactos
- [ ] Sincronizar contactos del teléfono
- [ ] Implementar búsqueda de usuarios
- [ ] Agregar/eliminar contactos
- [ ] Mostrar estado online de contactos

### 7.2 Grupos
- [ ] Crear nuevo grupo
- [ ] Agregar/quitar participantes
- [ ] Implementar roles (admin, miembro)
- [ ] Editar nombre, descripción, foto de grupo
- [ ] Implementar mensajes de sistema (X se unió, Y salió)
- [ ] Mostrar info de participantes
- [ ] Implementar salir de grupo

---

## FASE 8: PUSH NOTIFICATIONS (Semana 10-11)

### 8.1 Configuración FCM
- [ ] Configurar certificados iOS (APNs)
- [ ] Configurar Firebase Cloud Messaging
- [ ] Solicitar permisos de notificaciones
- [ ] Registrar token FCM con backend
- [ ] Manejar renovación de tokens

### 8.2 Notificaciones Locales
- [ ] Instalar @notifee/react-native
- [ ] Crear canales de notificación (Android)
- [ ] Implementar notificaciones de mensajes
- [ ] Agregar acciones (Responder, Marcar leído)
- [ ] Implementar respuesta rápida (inline reply)
- [ ] Mostrar imagen del remitente
- [ ] Agrupar notificaciones por chat

### 8.3 Manejo de Notificaciones
- [ ] Manejar tap en notificación (navegación)
- [ ] Actualizar badge count
- [ ] Limpiar notificaciones al abrir chat
- [ ] Implementar "Do Not Disturb" por chat
- [ ] Configurar sonidos personalizados

---

## FASE 9: OFFLINE Y SINCRONIZACIÓN (Semana 11-12)

### 9.1 Modo Offline
- [ ] Implementar detección de conectividad
- [ ] Mostrar banner de "sin conexión"
- [ ] Permitir leer mensajes offline
- [ ] Implementar cola de mensajes pendientes
- [ ] Guardar mensajes salientes en DB local

### 9.2 Sincronización
- [ ] Implementar sync al recuperar conexión
- [ ] Enviar mensajes pendientes en orden
- [ ] Descargar mensajes nuevos
- [ ] Resolver conflictos de sincronización
- [ ] Implementar retry con exponential backoff
- [ ] Sincronizar estados de mensajes

---

## FASE 10: CONFIGURACIÓN Y PERFIL (Semana 12-13)

### 10.1 Perfil de Usuario
- [ ] Crear pantalla de perfil
- [ ] Permitir editar nombre
- [ ] Cambiar foto de perfil (cámara/galería)
- [ ] Actualizar "acerca de"
- [ ] Mostrar QR code del usuario

### 10.2 Configuración
- [ ] Crear pantalla de configuración
- [ ] Implementar tema oscuro/claro
- [ ] Configurar notificaciones
- [ ] Ajustes de privacidad (última vez visto, foto, info)
- [ ] Configurar almacenamiento (auto-delete media)
- [ ] Bloquear/desbloquear usuarios
- [ ] Idiomas e internacionalización

---

## FASE 11: TESTING (Semana 13-14)

### 11.1 Unit Tests
- [ ] Tests de servicios (SignalR, WebRTC, API)
- [ ] Tests de stores/state management
- [ ] Tests de utilidades y helpers
- [ ] Tests de sincronización offline

### 11.2 Component Tests
- [ ] Tests de MessageBubble
- [ ] Tests de ChatList
- [ ] Tests de MessageInput
- [ ] Tests de CallControls

### 11.3 Integration Tests
- [ ] Test de flujo de autenticación
- [ ] Test de envío/recepción de mensajes
- [ ] Test de llamadas WebRTC
- [ ] Test de sincronización offline

### 11.4 E2E Tests (Detox)
- [ ] Setup Detox
- [ ] Test de login/registro
- [ ] Test de crear chat y enviar mensaje
- [ ] Test de llamada de voz
- [ ] Test de crear estado

---

## FASE 12: OPTIMIZACIÓN Y PULIDO (Semana 14-15)

### 12.1 Performance
- [ ] Optimizar renderizado de listas
- [ ] Implementar lazy loading de imágenes
- [ ] Reducir bundle size (tree shaking)
- [ ] Optimizar startup time
- [ ] Implementar code splitting
- [ ] Profiling con Flipper

### 12.2 Accesibilidad
- [ ] Agregar labels accesibles
- [ ] Soporte para screen readers
- [ ] Tamaño de fuente dinámico
- [ ] Contraste de colores adecuado
- [ ] Navegación con teclado

### 12.3 Internacionalización
- [ ] Configurar i18n (react-i18next)
- [ ] Extraer todos los strings
- [ ] Traducir a español/inglés
- [ ] Formateo de fechas/números según locale

### 12.4 Seguridad
- [ ] Ofuscar código (react-native-obfuscating-transformer)
- [ ] Implementar certificate pinning
- [ ] Validar inputs en frontend
- [ ] Proteger contra screenshots (chats privados)
- [ ] Implementar app lock (biometría/PIN)

---

## FASE 13: PREPARACIÓN PARA RELEASE (Semana 15-16)

### 13.1 Code Push / OTA Updates
- [ ] Configurar CodePush (Microsoft AppCenter)
- [ ] Implementar versionado semántico
- [ ] Crear flujo de deployment (dev, staging, prod)
- [ ] Implementar rollback automático en errores

### 13.2 Android Release
- [ ] Generar keystore de producción
- [ ] Configurar ProGuard/R8
- [ ] Optimizar APK/AAB size
- [ ] Crear assets (iconos, splash screens)
- [ ] Configurar versionCode y versionName
- [ ] Generar AAB firmado
- [ ] Preparar descripción para Play Store
- [ ] Screenshots y video preview
- [ ] Crear cuenta de desarrollador

### 13.3 iOS Release
- [ ] Configurar certificados de distribución
- [ ] Crear App ID y provisioning profiles
- [ ] Configurar capabilities (Push Notifications, CallKit, etc.)
- [ ] Optimizar IPA size
- [ ] Generar build de producción
- [ ] Preparar descripción para App Store
- [ ] Screenshots para diferentes tamaños
- [ ] App Store Connect setup
- [ ] Enviar para revisión

### 13.4 Monitoreo
- [ ] Integrar Sentry para error tracking
- [ ] Configurar Firebase Analytics
- [ ] Implementar custom events
- [ ] Crear dashboards de métricas
- [ ] Configurar alertas de errores críticos

---

## ESTIMACIÓN DE TIEMPO TOTAL: 14-16 SEMANAS (3.5-4 meses)

**Equipo sugerido:**
- 2 desarrolladores React Native senior
- 1 desarrollador backend (ajustes en API si necesario)
- 1 QA tester
- 1 diseñador UI/UX (part-time)

**Recursos:**
- 2 MacBooks (para builds de iOS)
- Dispositivos físicos Android e iOS para testing
- Cuentas de desarrollador (Apple $99/año, Google $25 one-time)
- Firebase plan Blaze (pay-as-you-go)
- Microsoft AppCenter para CodePush

---

## RIESGOS IDENTIFICADOS

### Riesgos Técnicos
1. **Complejidad de WebRTC en móvil**
   - Mitigación: Implementar primero en web, probar extensivamente
   - Tener plan B: usar servicio de terceros (Twilio, Agora)

2. **Sincronización offline compleja**
   - Mitigación: Implementar estrategia simple primero (queue)
   - Usar WatermelonDB que tiene sync built-in

3. **Performance en listas largas**
   - Mitigación: Usar FlashList desde el inicio
   - Implementar paginación agresiva

4. **Notificaciones push inconsistentes**
   - Mitigación: Implementar fallback a polling
   - Testing exhaustivo en diferentes dispositivos/OS

### Riesgos de Negocio
1. **Cambios en requisitos durante desarrollo**
   - Mitigación: Sprints cortos (2 semanas)
   - Demos frecuentes al stakeholder

2. **Dependencia de APIs de terceros**
   - Mitigación: Abstraer servicios (interfaces)
   - Tener proveedores alternativos identificados

---

## PRÓXIMOS PASOS

1. **Revisar y aprobar este plan** con el equipo
2. **Crear repositorio** y configurar Git workflow
3. **Setup de ambiente** de todos los desarrolladores
4. **Kickoff meeting** para alinear expectativas
5. **Comenzar Fase 1** - Sprint 1 (Semana 1-2)
