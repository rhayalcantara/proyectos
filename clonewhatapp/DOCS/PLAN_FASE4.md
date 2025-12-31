# Plan de Implementación - Fase 4: Estados/Stories

**Fecha de inicio**: 2025-12-30
**Objetivo**: Implementar la funcionalidad de Estados (Stories) similar a WhatsApp

---

## Descripción General

Los Estados permiten a los usuarios compartir contenido (texto, imágenes) que desaparece automáticamente después de 24 horas. Los contactos pueden ver estos estados y el creador puede ver quién los ha visto.

---

## Arquitectura

### Modelos de Base de Datos

```
Estado (EstadoUsuario)
├── Id (int, PK)
├── UsuarioId (int, FK → Usuario)
├── Tipo (enum: Texto, Imagen)
├── Contenido (string) - texto o ruta del archivo
├── ColorFondo (string, nullable) - para estados de texto
├── FechaCreacion (DateTime)
├── FechaExpiracion (DateTime) - FechaCreacion + 24h
└── Vistas (ICollection<VistaEstado>)

VistaEstado
├── Id (int, PK)
├── EstadoId (int, FK → Estado)
├── UsuarioId (int, FK → Usuario)
└── FechaVista (DateTime)
```

### Endpoints API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /api/estados | Publicar nuevo estado |
| POST | /api/estados/imagen | Publicar estado con imagen |
| GET | /api/estados | Obtener mis estados |
| GET | /api/estados/contactos | Obtener estados de mis contactos |
| GET | /api/estados/{id}/vistas | Ver quién vio mi estado |
| POST | /api/estados/{id}/vista | Marcar estado como visto |
| DELETE | /api/estados/{id} | Eliminar mi estado |

### Eventos SignalR

| Evento | Descripción |
|--------|-------------|
| NewStatus | Notificar a contactos de nuevo estado |
| StatusViewed | Notificar al creador que alguien vio su estado |
| StatusDeleted | Notificar eliminación de estado |

---

## Plan de Implementación

### Paso 1: Backend - Modelos (Tareas 4.1.1 - 4.1.2)

1. Crear modelo `Estado` en `/Models/Estado.cs`
2. Crear modelo `VistaEstado` en `/Models/VistaEstado.cs`
3. Crear DTOs necesarios:
   - `CrearEstadoDto`
   - `EstadoDto`
   - `VistaEstadoDto`
   - `EstadoContactoDto` (agrupa estados por contacto)
4. Actualizar `ApplicationDbContext` con nuevos DbSets
5. Generar migración de base de datos

### Paso 2: Backend - Endpoints (Tareas 4.1.3 - 4.1.5)

1. Crear `EstadosController`
2. Implementar POST `/api/estados` - Publicar estado de texto
3. Implementar POST `/api/estados/imagen` - Publicar estado con imagen
4. Implementar GET `/api/estados` - Obtener mis estados
5. Implementar GET `/api/estados/contactos` - Estados de contactos (agrupados)
6. Implementar POST `/api/estados/{id}/vista` - Marcar como visto
7. Implementar GET `/api/estados/{id}/vistas` - Ver quién vio
8. Implementar DELETE `/api/estados/{id}` - Eliminar estado

### Paso 3: Backend - Expiración (Tarea 4.1.6)

1. Crear servicio `EstadoCleanupService` (Background Service)
2. Ejecutar cada hora para eliminar estados expirados
3. También filtrar estados expirados en las consultas

### Paso 4: Backend - SignalR

1. Agregar eventos de estado al `ChatHub`
2. Notificar a contactos cuando se publica nuevo estado
3. Notificar al creador cuando alguien ve su estado

### Paso 5: Frontend - Servicios

1. Crear `StatusService` en Angular
2. Métodos para todas las operaciones CRUD
3. Integrar eventos SignalR

### Paso 6: Frontend - Componentes (Tareas 4.1.7 - 4.1.10)

1. Agregar pestaña "Estados" en el sidebar
2. Crear `StatusListComponent` - Lista de estados de contactos
3. Crear `CreateStatusComponent` - Modal para crear estado
4. Crear `StatusViewerComponent` - Visor carousel de estados
5. Crear `StatusViewersComponent` - Mostrar quién vio mi estado

---

## Estructura de Archivos a Crear/Modificar

### Backend (CloneWhatsApp.API)

```
CloneWhatsApp.API/
├── Models/
│   ├── Estado.cs (nuevo)
│   └── VistaEstado.cs (nuevo)
├── DTOs/
│   └── EstadoDtos.cs (nuevo)
├── Controllers/
│   └── EstadosController.cs (nuevo)
├── Services/
│   └── EstadoCleanupService.cs (nuevo)
├── Hubs/
│   └── ChatHub.cs (modificar)
└── Data/
    └── ApplicationDbContext.cs (modificar)
```

### Frontend (clone-whatsapp-web)

```
src/app/
├── core/services/
│   └── status.service.ts (nuevo)
└── features/
    └── status/
        ├── status.module.ts (nuevo)
        ├── components/
        │   ├── status-list/
        │   ├── create-status/
        │   ├── status-viewer/
        │   └── status-viewers/
        └── models/
            └── status.model.ts
```

---

## Diseño UI

### Lista de Estados (Sidebar)
- Mi estado en la parte superior (con botón de agregar)
- Estados recientes de contactos (con anillo de progreso)
- Estados ya vistos en gris

### Visor de Estados (Carousel)
- Pantalla completa con fondo oscuro
- Barra de progreso arriba
- Tap/swipe para siguiente
- Información del usuario y hora
- Responder con mensaje (futuro)

### Crear Estado
- Modal con opciones:
  - Estado de texto (elegir color de fondo)
  - Estado con imagen (subir/tomar foto)

---

## Consideraciones Técnicas

1. **Rendimiento**: Los estados expiran en 24h, usar índices en FechaExpiracion
2. **Almacenamiento**: Imágenes de estados en carpeta separada `/uploads/estados/`
3. **Privacidad**: Solo mostrar estados a contactos mutuos
4. **Limpieza**: Background service para eliminar estados y archivos expirados

---

## Lista de Tareas Detallada

| # | Tarea | Prioridad |
|---|-------|-----------|
| 1 | Crear modelos Estado y VistaEstado | Alta |
| 2 | Crear DTOs de estado | Alta |
| 3 | Actualizar DbContext y migración | Alta |
| 4 | Crear EstadosController con endpoints básicos | Alta |
| 5 | Implementar endpoint publicar estado texto | Alta |
| 6 | Implementar endpoint publicar estado imagen | Alta |
| 7 | Implementar endpoint obtener estados contactos | Alta |
| 8 | Implementar endpoint marcar como visto | Alta |
| 9 | Implementar endpoint ver quién vio | Media |
| 10 | Crear EstadoCleanupService | Media |
| 11 | Agregar eventos SignalR | Media |
| 12 | Crear StatusService en Angular | Alta |
| 13 | Agregar pestaña Estados en sidebar | Alta |
| 14 | Crear StatusListComponent | Alta |
| 15 | Crear CreateStatusComponent | Alta |
| 16 | Crear StatusViewerComponent (carousel) | Alta |
| 17 | Crear StatusViewersComponent | Media |
| 18 | Integrar eventos SignalR en frontend | Media |
| 19 | Pruebas e integración | Alta |

---

**Estado del plan**: Aprobado para implementación
