# Plan para Completar Fase 1 (100%)

**Fecha**: 2025-12-30
**Estado actual**: 93% completado
**Objetivo**: Completar las 4 tareas pendientes

---

## Tareas Pendientes

### Tarea 1: Subida de Foto de Perfil (Backend)
**Archivo a modificar**: `CloneWhatsApp.API/Controllers/UsersController.cs`

**Implementación**:
1. Crear carpeta `wwwroot/uploads/profiles` para almacenar imágenes
2. Agregar endpoint `POST /api/users/me/photo`
3. Validar tipo de archivo (solo imágenes)
4. Generar nombre único para el archivo
5. Guardar archivo en el servidor
6. Actualizar `FotoPerfil` del usuario con la URL

---

### Tarea 2: Componente de Perfil en Angular
**Archivos a crear**:
- `clone-whatsapp-web/src/app/features/profile/profile.component.ts`
- `clone-whatsapp-web/src/app/features/profile/profile.component.html`
- `clone-whatsapp-web/src/app/features/profile/profile.component.scss`

**Implementación**:
1. Crear componente standalone
2. Mostrar foto de perfil actual (o avatar por defecto)
3. Mostrar nombre y estado del usuario
4. Input para subir nueva foto
5. Botones para editar nombre y estado
6. Integrar en el sidebar del chat

---

### Tarea 3: Edición de Nombre y Estado
**Archivos a modificar**:
- Componente de perfil
- `auth.service.ts` (agregar método para subir foto)

**Implementación**:
1. Modal o campos editables inline
2. Llamar a `PUT /api/users/me` para actualizar
3. Actualizar estado local después de guardar
4. Validaciones de longitud máxima

---

### Tarea 4: Responsive Design Básico
**Archivos a modificar**:
- `chat-layout.component.scss`
- `chat-list.component.scss`
- `chat-window.component.scss`
- Agregar media queries

**Implementación**:
1. Breakpoints: mobile (<768px), tablet (768-1024px), desktop (>1024px)
2. En mobile: solo mostrar sidebar o chat (no ambos)
3. Agregar botón para volver a la lista en mobile
4. Header responsive
5. Input de mensaje responsive

---

## Orden de Ejecución

1. **Primero**: Backend (foto de perfil) - independiente
2. **Segundo**: Componente de perfil + edición
3. **Tercero**: Responsive design

---

## Archivos Clave del Proyecto

| Componente | Ruta |
|------------|------|
| Modelo Usuario | `CloneWhatsApp.API/Models/Usuario.cs` |
| UsersController | `CloneWhatsApp.API/Controllers/UsersController.cs` |
| AuthService | `clone-whatsapp-web/src/app/core/services/auth.service.ts` |
| ChatLayout | `clone-whatsapp-web/src/app/features/chat/components/chat-layout/` |

---

**Estado**: Listo para implementar
