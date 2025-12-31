# Plan: Unificar Frontend y Backend en un Solo Sitio

## Objetivo
Servir el frontend Angular y el backend .NET desde el mismo sitio (site1) para usar un solo certificado SSL.

## Arquitectura Final
```
https://rhayalcantara-002-site1.ntempurl.com/
├── /                    → Frontend Angular (archivos estáticos)
├── /api/*               → Backend API (.NET)
├── /hubs/chat           → SignalR Hub
└── /wwwroot/uploads/*   → Archivos subidos (fotos, etc.)
```

---

## Cambios Requeridos

### 1. BACKEND (Program.cs)

#### 1.1 Configurar servicio de archivos estáticos para SPA
Agregar middleware para servir el frontend Angular y manejar el fallback a `index.html` para las rutas del SPA.

**Archivo:** `CloneWhatsApp.API/Program.cs`

**Cambios:**
- Agregar `UseDefaultFiles()` antes de `UseStaticFiles()`
- Agregar fallback a `index.html` para rutas que no sean API/hubs
- Eliminar `UseHttpsRedirection()` (el hosting ya maneja HTTPS)

#### 1.2 Actualizar CORS (opcional - simplificar)
Como todo estará en el mismo origen, CORS ya no es estrictamente necesario, pero lo mantenemos para desarrollo local.

**Archivo:** `CloneWhatsApp.API/Program.cs`

**Cambio:** Mantener CORS solo para `localhost:4200`

---

### 2. FRONTEND (Environments)

#### 2.1 Cambiar URLs a rutas relativas
Las URLs del API deben ser relativas ya que estarán en el mismo origen.

**Archivo:** `clone-whatsapp-web/src/environments/environment.prod.ts`

**Antes:**
```typescript
apiUrl: 'https://rhayalcantara-002-site2.ntempurl.com/api',
hubUrl: 'https://rhayalcantara-002-site2.ntempurl.com/hubs/chat'
```

**Después:**
```typescript
apiUrl: '/api',
hubUrl: '/hubs/chat'
```

#### 2.2 Environment de desarrollo
Mantener URLs absolutas para desarrollo local.

**Archivo:** `clone-whatsapp-web/src/environments/environment.ts`

**Cambio:** Apuntar a `http://localhost:5000` o el puerto del backend local

---

### 3. BUILD Y DEPLOYMENT

#### 3.1 Build del Frontend
```bash
cd clone-whatsapp-web
ng build --configuration production
```

#### 3.2 Copiar archivos a wwwroot del Backend
Copiar el contenido de `dist/clone-whatsapp-web/browser/` a `CloneWhatsApp.API/wwwroot/`

#### 3.3 Build del Backend
```bash
cd CloneWhatsApp.API
dotnet publish -c Release -o publish
```

#### 3.4 Subir a SmarterASP.NET
Subir el contenido de `CloneWhatsApp.API/publish/` a site1

---

### 4. ELIMINAR WEB.CONFIG DEL FRONTEND
El `web.config` del frontend ya no es necesario porque el backend .NET manejará todo.

**Archivo a eliminar:** `clone-whatsapp-web/public/web.config`

---

### 5. CONFIGURACIÓN EN SMARTERASP.NET

#### 5.1 Site1 (único sitio a usar)
- Tipo: ASP.NET Core
- SSL: Activado (Let's Encrypt)
- Contenido: Backend + Frontend

#### 5.2 Site2 (ya no se usa)
- Puede desactivarse o eliminarse

---

## Archivos a Modificar

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `CloneWhatsApp.API/Program.cs` | Modificar | Agregar middleware SPA |
| `environment.prod.ts` | Modificar | URLs relativas |
| `environment.ts` | Modificar | URL localhost para dev |
| `public/web.config` | Eliminar | Ya no necesario |
| `CloneWhatsApp.API/wwwroot/` | Agregar | Archivos del frontend |

---

## Riesgos y Consideraciones

1. **Desarrollo local:** El frontend en desarrollo seguirá usando `ng serve` en puerto 4200, necesita proxy o CORS para conectar al backend.

2. **Caché del navegador:** Después del cambio, los usuarios pueden necesitar limpiar caché.

3. **Rutas conflictivas:** Asegurar que no haya conflicto entre rutas de Angular y rutas del API.

4. **Archivos estáticos existentes:** El backend ya tiene `wwwroot` con algunos archivos (uploads), hay que preservarlos.

---

## Orden de Ejecución Recomendado

1. Modificar `environment.prod.ts` (URLs relativas)
2. Modificar `environment.ts` (localhost para dev)
3. Modificar `Program.cs` (middleware SPA)
4. Eliminar `web.config` del frontend
5. Build del frontend
6. Copiar frontend a wwwroot
7. Build del backend
8. Deploy a site1
9. Probar funcionamiento
