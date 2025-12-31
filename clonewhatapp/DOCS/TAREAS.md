# Lista de Tareas - Clon de WhatsApp

## Leyenda de Estados
- [ ] Pendiente
- [🔄] En Progreso
- [✅] Finalizada

---

## FASE 1: FUNDAMENTOS (MVP)

### 1.1 Configuración del Proyecto

| # | Tarea | Estado |
|---|-------|--------|
| 1.1.1 | Crear solución de C# (.NET 9) | [✅] Finalizada |
| 1.1.2 | Configurar proyecto ASP.NET Core Web API | [✅] Finalizada |
| 1.1.3 | Configurar Entity Framework Core | [✅] Finalizada |
| 1.1.4 | Configurar base de datos MySQL | [✅] Finalizada |
| 1.1.5 | Crear proyecto Angular | [✅] Finalizada |
| 1.1.6 | Configurar estructura de carpetas Angular | [✅] Finalizada |
| 1.1.7 | Configurar CORS en backend | [✅] Finalizada |
| 1.1.8 | Configurar conexión Angular-Backend | [✅] Finalizada |

### 1.2 Autenticación

| # | Tarea | Estado |
|---|-------|--------|
| 1.2.1 | Crear modelo Usuario en backend | [✅] Finalizada |
| 1.2.2 | Crear DTOs de autenticación | [✅] Finalizada |
| 1.2.3 | Implementar registro de usuario | [✅] Finalizada |
| 1.2.4 | Implementar login con JWT | [✅] Finalizada |
| 1.2.5 | Crear middleware de autenticación | [✅] Finalizada |
| 1.2.6 | Crear servicio de auth en Angular | [✅] Finalizada |
| 1.2.7 | Crear componente de login | [✅] Finalizada |
| 1.2.8 | Crear componente de registro | [✅] Finalizada |
| 1.2.9 | Implementar guards de rutas | [✅] Finalizada |
| 1.2.10 | Implementar interceptor para JWT | [✅] Finalizada |

### 1.3 Perfil de Usuario

| # | Tarea | Estado |
|---|-------|--------|
| 1.3.1 | Crear endpoint GET perfil | [✅] Finalizada |
| 1.3.2 | Crear endpoint PUT actualizar perfil | [✅] Finalizada |
| 1.3.3 | Implementar subida de foto de perfil | [✅] Finalizada |
| 1.3.4 | Crear componente de perfil en Angular | [✅] Finalizada |
| 1.3.5 | Implementar edición de nombre y estado | [✅] Finalizada |

### 1.4 Contactos

| # | Tarea | Estado |
|---|-------|--------|
| 1.4.1 | Crear modelo Contacto | [✅] Finalizada |
| 1.4.2 | Crear endpoint listar contactos | [✅] Finalizada |
| 1.4.3 | Crear endpoint agregar contacto | [✅] Finalizada |
| 1.4.4 | Crear endpoint eliminar contacto | [✅] Finalizada |
| 1.4.5 | Crear componente lista de contactos | [✅] Finalizada |
| 1.4.6 | Crear componente agregar contacto | [✅] Finalizada |
| 1.4.7 | Implementar búsqueda de contactos | [✅] Finalizada |

### 1.5 Chat Individual (Texto)

| # | Tarea | Estado |
|---|-------|--------|
| 1.5.1 | Crear modelo Chat | [✅] Finalizada |
| 1.5.2 | Crear modelo Mensaje | [✅] Finalizada |
| 1.5.3 | Crear modelo ChatParticipante | [✅] Finalizada |
| 1.5.4 | Configurar SignalR en backend | [✅] Finalizada |
| 1.5.5 | Crear ChatHub para WebSockets | [✅] Finalizada |
| 1.5.6 | Implementar conexión/desconexión de usuarios | [✅] Finalizada |
| 1.5.7 | Crear endpoint obtener chats del usuario | [✅] Finalizada |
| 1.5.8 | Crear endpoint crear chat | [✅] Finalizada |
| 1.5.9 | Crear endpoint obtener mensajes de un chat | [✅] Finalizada |
| 1.5.10 | Implementar envío de mensajes vía SignalR | [✅] Finalizada |
| 1.5.11 | Implementar recepción de mensajes en tiempo real | [✅] Finalizada |
| 1.5.12 | Crear servicio SignalR en Angular | [✅] Finalizada |
| 1.5.13 | Crear componente lista de chats | [✅] Finalizada |
| 1.5.14 | Crear componente conversación | [✅] Finalizada |
| 1.5.15 | Crear componente input de mensaje | [✅] Finalizada |
| 1.5.16 | Crear componente burbuja de mensaje | [✅] Finalizada |
| 1.5.17 | Implementar scroll automático | [✅] Finalizada |
| 1.5.18 | Implementar indicador "escribiendo..." | [✅] Finalizada (backend) |

### 1.6 UI/UX Básico

| # | Tarea | Estado |
|---|-------|--------|
| 1.6.1 | Diseñar layout principal (sidebar + chat) | [✅] Finalizada |
| 1.6.2 | Implementar tema visual estilo WhatsApp | [✅] Finalizada |
| 1.6.3 | Crear header de chat | [✅] Finalizada |
| 1.6.4 | Implementar responsive design básico | [✅] Finalizada |

---

## FASE 2: MENSAJERÍA COMPLETA

### 2.1 Estados de Mensaje

| # | Tarea | Estado |
|---|-------|--------|
| 2.1.1 | Crear modelo EstadoMensaje | [✅] Finalizada |
| 2.1.2 | Implementar lógica de estado "enviado" | [✅] Finalizada |
| 2.1.3 | Implementar lógica de estado "entregado" | [✅] Finalizada |
| 2.1.4 | Implementar lógica de estado "leído" | [✅] Finalizada |
| 2.1.5 | Mostrar checks de estado en UI | [✅] Finalizada |
| 2.1.6 | Implementar notificación de lectura | [✅] Finalizada |

### 2.2 Mensajes Multimedia

| # | Tarea | Estado |
|---|-------|--------|
| 2.2.1 | Configurar almacenamiento de archivos | [✅] Finalizada |
| 2.2.2 | Crear endpoint subir archivo | [✅] Finalizada |
| 2.2.3 | Implementar envío de imágenes | [✅] Finalizada |
| 2.2.4 | Implementar envío de documentos | [✅] Finalizada |
| 2.2.5 | Crear previsualizador de imágenes | [✅] Finalizada |
| 2.2.6 | Implementar descarga de archivos | [✅] Finalizada |
| 2.2.7 | Comprimir imágenes antes de enviar | [ ] Pendiente (opcional) |

### 2.3 Funcionalidades Adicionales de Mensajes

| # | Tarea | Estado |
|---|-------|--------|
| 2.3.1 | Implementar responder mensaje | [✅] Finalizada |
| 2.3.2 | Implementar eliminar mensaje para mí | [✅] Finalizada |
| 2.3.3 | Implementar eliminar mensaje para todos | [✅] Finalizada |
| 2.3.4 | Implementar reenviar mensaje | [✅] Finalizada |
| 2.3.5 | Implementar copiar mensaje | [✅] Finalizada |
| 2.3.6 | Implementar búsqueda en chat | [✅] Finalizada |

---

## FASE 3: GRUPOS

### 3.1 Gestión de Grupos

| # | Tarea | Estado |
|---|-------|--------|
| 3.1.1 | Crear modelo Grupo | [✅] Finalizada |
| 3.1.2 | Crear endpoint crear grupo | [✅] Finalizada |
| 3.1.3 | Crear endpoint obtener detalles grupo | [✅] Finalizada |
| 3.1.4 | Crear endpoint actualizar grupo | [✅] Finalizada |
| 3.1.5 | Implementar agregar participantes | [✅] Finalizada |
| 3.1.6 | Implementar eliminar participantes | [✅] Finalizada |
| 3.1.7 | Implementar cambiar rol (admin) | [✅] Finalizada |
| 3.1.8 | Implementar salir del grupo | [✅] Finalizada |
| 3.1.9 | Crear componente crear grupo | [✅] Finalizada |
| 3.1.10 | Crear componente info del grupo | [✅] Finalizada |
| 3.1.11 | Crear componente lista participantes | [✅] Finalizada |

### 3.2 Mensajería Grupal

| # | Tarea | Estado |
|---|-------|--------|
| 3.2.1 | Adaptar SignalR para grupos | [✅] Finalizada |
| 3.2.2 | Implementar envío a múltiples usuarios | [✅] Finalizada |
| 3.2.3 | Mostrar nombre del remitente en grupo | [✅] Finalizada |
| 3.2.4 | Implementar mensajes de sistema (entró, salió) | [✅] Finalizada |

---

## FASE 4: ESTADOS/STORIES

### 4.1 Estados

| # | Tarea | Estado |
|---|-------|--------|
| 4.1.1 | Crear modelo EstadoUsuario | [✅] Finalizada |
| 4.1.2 | Crear modelo VistaEstado | [✅] Finalizada |
| 4.1.3 | Crear endpoint publicar estado | [✅] Finalizada |
| 4.1.4 | Crear endpoint obtener estados de contactos | [✅] Finalizada |
| 4.1.5 | Crear endpoint marcar estado como visto | [✅] Finalizada |
| 4.1.6 | Implementar expiración de estados (24h) | [✅] Finalizada |
| 4.1.7 | Crear componente publicar estado | [✅] Finalizada |
| 4.1.8 | Crear componente ver estados | [✅] Finalizada |
| 4.1.9 | Crear visor de estados (carousel) | [✅] Finalizada |
| 4.1.10 | Mostrar quién vio mi estado | [✅] Finalizada |

---

## FASE 5: CARACTERÍSTICAS AVANZADAS

### 5.1 Mensajes de Voz

| # | Tarea | Estado |
|---|-------|--------|
| 5.1.1 | Implementar grabación de audio en frontend | [✅] Finalizada |
| 5.1.2 | Implementar subida de audio | [✅] Finalizada |
| 5.1.3 | Crear reproductor de audio | [✅] Finalizada |
| 5.1.4 | Mostrar duración y forma de onda | [✅] Finalizada |

### 5.2 Llamadas (WebRTC)

| # | Tarea | Estado |
|---|-------|--------|
| 5.2.1 | Configurar WebRTC en backend | [✅] Finalizada |
| 5.2.2 | Implementar señalización | [✅] Finalizada |
| 5.2.3 | Implementar llamada de voz | [✅] Finalizada |
| 5.2.4 | Implementar videollamada | [✅] Finalizada |
| 5.2.5 | Crear UI de llamada entrante | [✅] Finalizada |
| 5.2.6 | Crear UI durante llamada | [✅] Finalizada |
| 5.2.7 | Historial de llamadas | [✅] Finalizada |

### 5.3 Otras Mejoras

| # | Tarea | Estado |
|---|-------|--------|
| 5.3.1 | Implementar notificaciones push | [✅] Finalizada |
| 5.3.2 | Implementar modo oscuro | [✅] Finalizada |
| 5.3.3 | Implementar bloquear usuario | [✅] Finalizada |
| 5.3.4 | Implementar silenciar chat | [✅] Finalizada |
| 5.3.5 | Implementar archivar chat | [✅] Finalizada |
| 5.3.6 | Implementar cifrado end-to-end | [ ] Pendiente |

---

## Resumen de Progreso

| Fase | Total Tareas | Completadas | Progreso |
|------|--------------|-------------|----------|
| Fase 1 | 54 | 54 | 100% |
| Fase 2 | 19 | 19 | 100% |
| Fase 3 | 15 | 15 | 100% |
| Fase 4 | 10 | 10 | 100% |
| Fase 5 | 17 | 16 | 94% |
| **TOTAL** | **115** | **114** | **99%** |

---

**Última actualización**: 2025-12-30

### Notas de la sesión:
- Se completó el MVP con autenticación, contactos y chat individual
- Frontend corriendo en http://localhost:4200
- Backend corriendo en http://localhost:5181
- Base de datos MySQL configurada correctamente
- SignalR configurado para mensajería en tiempo real
- Modal de nuevo chat implementado con lista de contactos y agregar contacto
- **FASE 1 COMPLETADA AL 100%**:
  - Endpoint para subida de foto de perfil (POST /api/users/me/photo)
  - Endpoint para eliminar foto de perfil (DELETE /api/users/me/photo)
  - Componente de perfil completo con edición de nombre y estado
  - Responsive design implementado para mobile, tablet y desktop
