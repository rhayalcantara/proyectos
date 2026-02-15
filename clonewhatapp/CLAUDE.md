# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CloneWhatsApp** — A full-stack real-time messaging application (WhatsApp clone) with voice/video calls, stories, and push notifications. The codebase uses **Spanish** for variable names, models, comments, and API endpoints.

## Tech Stack

- **Backend:** ASP.NET Core (.NET 9.0), Entity Framework Core, MySQL, SignalR, JWT auth, WebPush
- **Frontend:** Angular 19 (standalone components), TypeScript, SCSS, @microsoft/signalr, WebRTC
- **Real-time:** SignalR WebSocket hub at `/hubs/chat`
- **Calls:** WebRTC peer connections with STUN servers, SDP offer/answer via SignalR

## Common Commands

### Backend (.NET)
```bash
# Run the API server
dotnet run --project CloneWhatsApp.API/CloneWhatsApp.API.csproj

# Build
dotnet build CloneWhatsApp.API/CloneWhatsApp.API.csproj

# Publish for production
dotnet publish CloneWhatsApp.API/CloneWhatsApp.API.csproj -c Release

# Apply EF Core migrations
dotnet ef database update --project CloneWhatsApp.API
```

### Frontend (Angular)
```bash
cd clone-whatsapp-web

# Install dependencies
npm install

# Dev server with HTTPS (configured in angular.json)
ng serve --ssl

# Production build
ng build

# Run a specific component's tests
ng test --include='**/chat-window*'
```

### HTTPS / Certificates
Self-signed certs live in `certs/` (cert.pem, key.pem, cert.pfx). Angular uses PEM files; Kestrel uses PFX.

## Architecture

```
┌──────────────────────────────────────────────────┐
│  Angular 19 SPA (clone-whatsapp-web/)            │
│  ├── features/auth     – Login, Register         │
│  ├── features/chat     – Chat list, window,      │
│  │                       calls, audio player      │
│  ├── features/status   – Stories/Status           │
│  ├── features/contacts – Contact management       │
│  ├── features/profile  – User profile             │
│  ├── features/settings – App settings             │
│  └── core/services     – 13 injectable services   │
├──────────────────────────────────────────────────┤
│  ASP.NET Core API (CloneWhatsApp.API/)           │
│  ├── Controllers/  – 8 REST controllers          │
│  ├── Hubs/ChatHub  – SignalR hub (28 methods)    │
│  ├── Services/     – Auth, Push, Cleanup         │
│  ├── Models/       – 11 EF Core entities         │
│  ├── DTOs/         – Request/Response objects     │
│  └── Data/         – ApplicationDbContext         │
├──────────────────────────────────────────────────┤
│  MySQL Database                                   │
└──────────────────────────────────────────────────┘
```

### Communication Pattern

- **REST API** (`/api/*`) for CRUD operations (chats, messages, contacts, auth, calls, status, push)
- **SignalR Hub** (`/hubs/chat`) for real-time events: messaging, typing indicators, online status, WebRTC signaling
- **WebRTC** for peer-to-peer voice/video, signaled through ChatHub
- **JWT** tokens sent via `Authorization: Bearer` header (HTTP) and `?access_token=` query param (SignalR)

### Key Design Patterns

- **Single SignalR connection** per user session; user joins their chat groups on connect
- **Frontend services** are singleton injectables in `core/services/`
- **Backend uses `asyncio.to_thread` pattern** — MCP tools bridge async to sync DBManager
- **Message status tracking** per-user via `EstadoMensajeUsuario` join table (sent → delivered → read)
- **Stories auto-expire** via `EstadoCleanupService` hosted background service

### Environment Configuration

| Setting | Development | Production |
|---------|-------------|------------|
| API URL | `https://192.168.1.170:7231/api` | `/api` (same origin) |
| Hub URL | `https://192.168.1.170:7231/hubs/chat` | `/hubs/chat` |
| Angular port | 4200 (HTTPS) | Served from wwwroot |
| .NET port | 7231 (HTTPS) | Configured in host |

Environment files: `clone-whatsapp-web/src/environments/environment.ts` and `environment.prod.ts`

### Database Entities (Spanish names)

`Usuarios`, `Chats`, `ChatParticipantes`, `Mensajes`, `EstadoMensajeUsuario`, `Estados`, `VistasEstados`, `Llamadas`, `UsuariosBloqueados`, `SuscripcionesPush`, `Contactos`

### Message Types (TipoMensaje enum)

Texto, Imagen, Video, Audio, Documento, Ubicacion, Contacto, Sistema

### Call Flow (WebRTC via SignalR)

1. `CallUser()` → sends SDP offer to target
2. `ReceiveCallOffer` → target receives offer
3. `AnswerCall()` → target sends SDP answer
4. ICE candidates exchanged via `SendICECandidate()`
5. `EndCall()` / `RejectCall()` to terminate

## Important Constraints

1. **CORS origins** are hardcoded in `Program.cs` — must update when deploying to new domains
2. **File upload limit** is 50 MB (configured in Program.cs)
3. **Database** uses `EnsureCreated()` on startup — migrations exist but aren't auto-applied
4. **Block validation** happens in ChatHub before allowing calls
5. **Production deployment** serves Angular SPA from `wwwroot/` via `UseStaticFiles` + SPA fallback
