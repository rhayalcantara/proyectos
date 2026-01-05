# Plan de Configuracion HTTPS para Desarrollo Local

## Objetivo
Configurar HTTPS en desarrollo local para que WebRTC funcione correctamente desde otros dispositivos en la red local. `getUserMedia()` requiere un contexto seguro (HTTPS o localhost).

---

## Analisis de Opciones

### 1. mkcert (Recomendada)

**Descripcion:** Herramienta que crea certificados SSL de desarrollo localmente confiables sin configuracion compleja.

**Pros:**
- Instalacion simple con un solo comando
- Los certificados son automaticamente confiables en el sistema local
- Sin advertencias de seguridad en el navegador
- Soporta multiples hostnames e IPs en un solo certificado
- Funciona en macOS, Windows y Linux
- Puede crear certificados para IPs de red local (ej: 192.168.1.x)
- Se puede instalar la CA raiz en dispositivos moviles (iOS/Android)
- Gratis y open source

**Contras:**
- Requiere instalar la CA raiz en cada dispositivo que acceda
- La CA raiz debe protegerse (no compartir rootCA-key.pem)
- Configuracion adicional necesaria para moviles

---

### 2. dotnet dev-certs

**Descripcion:** Herramienta incluida en el SDK de .NET para crear certificados de desarrollo HTTPS.

**Pros:**
- Ya incluida en el SDK de .NET
- Integracion nativa con ASP.NET Core
- Facil de usar para desarrollo local basico

**Contras:**
- Solo genera certificados para localhost
- No soporta IPs de red local directamente
- No funciona para acceso desde otros dispositivos
- Requiere pasos adicionales para exportar y compartir

---

### 3. Certificados Autofirmados con OpenSSL

**Descripcion:** Crear certificados manualmente usando OpenSSL.

**Pros:**
- Control total sobre la configuracion
- Puede incluir cualquier hostname o IP
- No requiere herramientas adicionales (OpenSSL viene en macOS)

**Contras:**
- Configuracion compleja y propensa a errores
- Los navegadores muestran advertencias de seguridad
- Requiere importar manualmente en cada dispositivo
- Proceso tedioso y repetitivo

---

### 4. Angular --ssl Flag

**Descripcion:** Usar la opcion ssl integrada en Angular CLI.

**Pros:**
- Facil de activar con un flag
- No requiere configuracion adicional para localhost

**Contras:**
- Genera certificado autofirmado
- Navegadores muestran advertencias
- Solo para Angular, no resuelve el backend

---

### 5. ngrok

**Descripcion:** Servicio de tuneles que expone servicios locales a Internet con HTTPS valido.

**Pros:**
- Certificados SSL validos y confiables
- Funciona desde cualquier dispositivo sin configuracion
- Ideal para demos y pruebas rapidas
- No requiere configuracion de red

**Contras:**
- Requiere conexion a Internet
- Latencia adicional (trafico pasa por servidores externos)
- Plan gratuito limitado (URLs temporales)
- Puede ser lento para desarrollo diario
- Costo mensual para funciones avanzadas

---

### 6. Cloudflare Tunnel

**Descripcion:** Alternativa gratuita a ngrok de Cloudflare.

**Pros:**
- Completamente gratuito
- Sin limites de ancho de banda
- SSL automatico
- CDN incluido

**Contras:**
- Requiere cuenta de Cloudflare
- Configuracion mas compleja que ngrok
- Requiere mover DNS a Cloudflare para dominios personalizados
- Requiere conexion a Internet

---

## Recomendacion: mkcert + Configuracion Kestrel/Angular

**Por que mkcert es la mejor opcion:**
1. Funciona sin Internet (desarrollo offline)
2. Sin latencia adicional
3. Certificados confiables sin advertencias
4. Soporta IPs de red local
5. Una sola configuracion para backend y frontend
6. Gratis y sin limites

---

## Plan de Implementacion

### Lista de Tareas

| # | Tarea | Estado |
|---|-------|--------|
| 1 | Instalar mkcert en macOS | Pendiente |
| 2 | Crear CA local e instalarla | Pendiente |
| 3 | Generar certificados para localhost e IP local | Pendiente |
| 4 | Configurar ASP.NET Core para usar certificados | Pendiente |
| 5 | Configurar Angular para usar certificados | Pendiente |
| 6 | Probar acceso desde dispositivo movil | Pendiente |
| 7 | Instalar CA raiz en dispositivos moviles (opcional) | Pendiente |

---

## Pasos Detallados de Implementacion

### Paso 1: Instalar mkcert en macOS

```bash
# Instalar mkcert via Homebrew
brew install mkcert

# Para soporte de Firefox (opcional)
brew install nss
```

### Paso 2: Crear e Instalar la CA Local

```bash
# Crear e instalar la CA local en el sistema
mkcert -install

# Verificar ubicacion de la CA (para compartir con moviles)
mkcert -CAROOT
```

Esto crea una Autoridad Certificadora (CA) local y la instala en el almacen de certificados del sistema.

### Paso 3: Generar Certificados

Primero, obtener tu IP local:
```bash
# En macOS
ipconfig getifaddr en0
# O
ifconfig | grep "inet " | grep -v 127.0.0.1
```

Luego, crear los certificados:
```bash
# Crear directorio para certificados en el proyecto
mkdir -p /Users/rhayalcantara/proyectos/clonewhatapp/certs

# Generar certificados (reemplazar 192.168.x.x con tu IP real)
cd /Users/rhayalcantara/proyectos/clonewhatapp/certs
mkcert -key-file key.pem -cert-file cert.pem localhost 127.0.0.1 ::1 192.168.1.100

# Tambien crear version PFX para .NET
mkcert -pkcs12 -p12-file cert.pfx localhost 127.0.0.1 ::1 192.168.1.100
```

**Nota:** La contrasena por defecto del archivo .pfx es `changeit`

### Paso 4: Configurar ASP.NET Core (Backend)

#### Opcion A: Configurar en appsettings.Development.json

Editar `/Users/rhayalcantara/proyectos/clonewhatapp/CloneWhatsApp.API/appsettings.Development.json`:

```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  },
  "Kestrel": {
    "Endpoints": {
      "Https": {
        "Url": "https://0.0.0.0:7231",
        "Certificate": {
          "Path": "../certs/cert.pfx",
          "Password": "changeit"
        }
      },
      "Http": {
        "Url": "http://0.0.0.0:5181"
      }
    }
  }
}
```

#### Opcion B: Configurar en launchSettings.json

Editar `/Users/rhayalcantara/proyectos/clonewhatapp/CloneWhatsApp.API/Properties/launchSettings.json`:

```json
{
  "$schema": "https://json.schemastore.org/launchsettings.json",
  "profiles": {
    "https": {
      "commandName": "Project",
      "dotnetRunMessages": true,
      "launchBrowser": false,
      "applicationUrl": "https://0.0.0.0:7231;http://0.0.0.0:5181",
      "environmentVariables": {
        "ASPNETCORE_ENVIRONMENT": "Development",
        "ASPNETCORE_Kestrel__Certificates__Default__Path": "../certs/cert.pfx",
        "ASPNETCORE_Kestrel__Certificates__Default__Password": "changeit"
      }
    }
  }
}
```

### Paso 5: Configurar Angular (Frontend)

#### 5.1 Actualizar angular.json

Editar `/Users/rhayalcantara/proyectos/clonewhatapp/clone-whatsapp-web/angular.json`:

Agregar las opciones SSL en la seccion `serve`:

```json
"serve": {
  "builder": "@angular-devkit/build-angular:dev-server",
  "options": {
    "ssl": true,
    "sslKey": "../certs/key.pem",
    "sslCert": "../certs/cert.pem",
    "host": "0.0.0.0",
    "port": 4200
  },
  "configurations": {
    "production": {
      "buildTarget": "clone-whatsapp-web:build:production"
    },
    "development": {
      "buildTarget": "clone-whatsapp-web:build:development"
    }
  },
  "defaultConfiguration": "development"
}
```

#### 5.2 Comando alternativo (sin modificar angular.json)

```bash
cd /Users/rhayalcantara/proyectos/clonewhatapp/clone-whatsapp-web
ng serve --ssl --ssl-key ../certs/key.pem --ssl-cert ../certs/cert.pem --host 0.0.0.0
```

### Paso 6: Probar desde Otro Dispositivo

1. Obtener IP de tu Mac: `ipconfig getifaddr en0`
2. Desde otro dispositivo en la misma red, acceder a:
   - Frontend: `https://192.168.x.x:4200`
   - Backend: `https://192.168.x.x:7231`

### Paso 7: Instalar CA en Dispositivos Moviles (Opcional)

#### Para iOS (iPhone/iPad):

1. Obtener el archivo CA raiz:
   ```bash
   # Ver ubicacion
   mkcert -CAROOT
   # Ejemplo: /Users/rhayalcantara/Library/Application Support/mkcert
   ```

2. Enviar `rootCA.pem` al dispositivo via:
   - AirDrop (mas facil)
   - Email (abrir con Mail de Apple)
   - Safari (servir el archivo desde un servidor HTTP local)

3. En el iPhone:
   - Abrir el archivo recibido
   - Ir a Configuracion > Perfil Descargado
   - Instalar el perfil
   - Ir a Configuracion > General > Informacion > Conf. del certificado
   - Activar "Confianza total" para mkcert

#### Para Android:

1. Copiar `rootCA.pem` al dispositivo

2. Ir a Configuracion > Seguridad > Cifrado y credenciales > Instalar certificado

3. Seleccionar "CA certificate" e instalar el archivo

**Nota:** Las apps de Android por defecto no confian en certificados de usuario. Para desarrollo, puedes usar Chrome que si los acepta.

---

## Configuracion Completa - Resumen de Archivos

### Estructura de carpetas sugerida:
```
clonewhatapp/
├── certs/
│   ├── cert.pem
│   ├── cert.pfx
│   └── key.pem
├── CloneWhatsApp.API/
│   └── appsettings.Development.json (modificado)
└── clone-whatsapp-web/
    └── angular.json (modificado)
```

### Script de configuracion rapida:

```bash
#!/bin/bash
# setup-https-local.sh

# Obtener IP local
LOCAL_IP=$(ipconfig getifaddr en0)
echo "IP Local detectada: $LOCAL_IP"

# Crear directorio de certificados
mkdir -p /Users/rhayalcantara/proyectos/clonewhatapp/certs
cd /Users/rhayalcantara/proyectos/clonewhatapp/certs

# Generar certificados con mkcert
mkcert -key-file key.pem -cert-file cert.pem localhost 127.0.0.1 ::1 $LOCAL_IP
mkcert -pkcs12 -p12-file cert.pfx localhost 127.0.0.1 ::1 $LOCAL_IP

echo ""
echo "Certificados generados en: $(pwd)"
echo ""
echo "Para acceder desde otros dispositivos:"
echo "  Frontend: https://$LOCAL_IP:4200"
echo "  Backend:  https://$LOCAL_IP:7231"
```

---

## Notas de Seguridad

1. **NUNCA compartir** `rootCA-key.pem` - da control total sobre tu CA
2. **NO usar** estos certificados en produccion
3. Los archivos `.pem` y `.pfx` no deben ir al repositorio git (agregar a `.gitignore`)
4. La CA raiz solo es confiable en dispositivos donde se instale manualmente

---

## Alternativa Rapida: ngrok (Para Pruebas Puntuales)

Si solo necesitas probar rapidamente sin configurar certificados:

```bash
# Instalar ngrok
brew install ngrok

# Exponer el backend
ngrok http 5181

# Exponer el frontend (en otra terminal)
ngrok http 4200
```

ngrok proporciona URLs HTTPS publicas temporales que funcionan desde cualquier dispositivo.

---

## Referencias

- [mkcert GitHub](https://github.com/FiloSottile/mkcert)
- [ASP.NET Core HTTPS - Scott Hanselman](https://www.hanselman.com/blog/developing-locally-with-aspnet-core-under-https-ssl-and-selfsigned-certs)
- [Angular HTTPS Setup](https://betterprogramming.pub/how-to-serve-your-angular-application-over-https-using-ng-serve-240e2c2e0a5d)
- [Kestrel Endpoints Configuration](https://learn.microsoft.com/en-us/aspnet/core/fundamentals/servers/kestrel/endpoints)
- [WebRTC Local Development](https://www.daily.co/blog/setting-up-a-local-webrtc-development-environment/)
- [dotnet dev-certs](https://learn.microsoft.com/en-us/dotnet/core/tools/dotnet-dev-certs)
- [ngrok Alternatives](https://pinggy.io/blog/best_ngrok_alternatives/)
