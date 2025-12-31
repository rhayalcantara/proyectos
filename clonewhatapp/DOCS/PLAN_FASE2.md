# Plan Fase 2: Mensajería Completa

**Fecha**: 2025-12-30
**Total tareas**: 19

---

## 2.1 Estados de Mensaje (6 tareas)

### Objetivo
Implementar el sistema de doble check (✓✓) estilo WhatsApp para mostrar el estado de cada mensaje.

### Estados
- **Enviado** (✓): Mensaje enviado al servidor
- **Entregado** (✓✓ gris): Mensaje recibido por el destinatario
- **Leído** (✓✓ azul): Mensaje visto por el destinatario

### Implementación

1. **Backend**:
   - Ya existe el modelo `EstadoMensajeUsuario` para tracking por usuario
   - Agregar eventos SignalR para notificar cambios de estado
   - Endpoint para marcar mensajes como leídos

2. **Frontend**:
   - Mostrar íconos de estado en cada mensaje
   - Escuchar eventos de cambio de estado via SignalR

---

## 2.2 Mensajes Multimedia (7 tareas)

### Objetivo
Permitir enviar imágenes y documentos en los chats.

### Implementación

1. **Backend**:
   - Configurar carpeta `wwwroot/uploads/messages`
   - Endpoint para subir archivos (POST /api/messages/upload)
   - Validación de tipos de archivo
   - Límite de tamaño (10MB)

2. **Frontend**:
   - Botón para adjuntar archivos
   - Preview de imagen antes de enviar
   - Visualizador de imágenes en chat
   - Descarga de documentos

---

## 2.3 Funcionalidades Adicionales (6 tareas)

### Objetivo
Agregar interacciones avanzadas con mensajes.

### Implementación

1. **Responder mensaje**: Quote del mensaje original
2. **Eliminar para mí**: Ocultar mensaje localmente
3. **Eliminar para todos**: Flag `eliminadoParaTodos`
4. **Reenviar mensaje**: Copiar a otro chat
5. **Copiar mensaje**: Copiar texto al portapapeles
6. **Buscar en chat**: Filtrar mensajes por texto

---

## Orden de Ejecución

1. **Primero**: Estados de mensaje (fundamental para UX)
2. **Segundo**: Mensajes multimedia (funcionalidad clave)
3. **Tercero**: Funcionalidades adicionales (mejoras de UX)

---

**Estado**: En progreso
