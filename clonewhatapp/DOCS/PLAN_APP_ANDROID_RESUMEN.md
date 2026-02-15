# Plan App Android - Resumen Comparativo de 3 Enfoques

## Contexto

Tenemos un backend ASP.NET Core funcionando con:
- REST API + SignalR (WebSocket) + WebRTC (llamadas) + JWT auth + MySQL
- Frontend Angular 19 funcionando

Necesitamos una app Android. Tres agentes propusieron enfoques distintos.

---

## Opción A: Kotlin Nativo (Android puro)

**Stack:** Jetpack Compose + Hilt + Retrofit + Room + SignalR Java Client + Stream WebRTC

### Ventajas
- **Mejor rendimiento posible** — acceso directo a todas las APIs de Android
- **SignalR resuelto** — cliente oficial Java de Microsoft (`com.microsoft.signalr:signalr:8.0.0`)
- **WebRTC maduro** — Stream WebRTC Android o Google WebRTC nativo
- **UX premium** — Material You, Dynamic Colors, Bubble notifications, PiP, Predictive Back
- **Ecosistema estable** — Jetpack Compose es la dirección oficial de Google
- **Sin puentes** — todo es nativo, sin overhead de comunicación entre capas

### Desventajas
- **Solo Android** — si luego quieres iOS, hay que escribir otra app desde cero
- **Timeline mayor** — 18-23 semanas (4.5-6 meses)
- **Requiere dev Kotlin/Android experimentado**

### Riesgos
- WebRTC NAT traversal (necesita TURN server)
- Battery drain con SignalR persistente

### Costo estimado: ~$80-100k | Timeline: 18-23 semanas

---

## Opción B: React Native (Cross-platform)

**Stack:** React Native + @microsoft/signalr + react-native-webrtc + Zustand + WatermelonDB

### Ventajas
- **Mismo paquete SignalR que el web** — `@microsoft/signalr` funciona directo (ya lo usas en Angular)
- **Reutilización de conocimiento** — si tu equipo sabe TypeScript/Angular, la curva es mínima
- **Android + iOS** con ~85% código compartido
- **OTA updates** — CodePush permite actualizar sin pasar por Play Store
- **Timeline más corto** — 16-18 semanas (4 meses)
- **Hot reload** — desarrollo más rápido
- **Comunidad enorme** — react-native-webrtc tiene 30k+ estrellas

### Desventajas
- **UX no 100% nativa** — necesita trabajo extra para que se sienta premium
- **Bridge overhead** — comunicación JS↔Native tiene latencia
- **Dependencia de paquetes community** — algunos pueden quedar sin mantenimiento
- **Debugging más complejo** — errores pueden estar en JS, native, o el bridge
- **Tamaño APK mayor** (~25-30MB vs ~15MB nativo)

### Riesgos
- Performance en listas largas de mensajes (mitigable con FlashList)
- react-native-webrtc puede tener bugs edge-case
- Dependencia de terceros para funcionalidades nativas

### Costo estimado: ~$60-100k | Timeline: 16-18 semanas

---

## Opción C: Kotlin Multiplatform (KMP)

**Stack:** KMP shared + Jetpack Compose (Android) + Ktor + SQLDelight + Koin

### Ventajas
- **Lógica compartida** — domain/data layer reutilizable entre Android e iOS
- **UI nativa real** — Compose en Android, SwiftUI en iOS
- **Kotlin everywhere** — mismo lenguaje para backend .NET... no, solo para mobile
- **Respaldado por JetBrains y Google** — futuro prometedor

### Desventajas
- **NO hay cliente SignalR para KMP** — hay que crear wrapper expect/actual (RIESGO ALTO)
- **WebRTC totalmente platform-specific** — solo ~9% del código de llamadas es compartible
- **Código compartido real: ~50-65%** — menor de lo prometido por el marketing
- **Ecosistema inmaduro para real-time** — pocas librerías probadas en producción
- **Timeline más largo** — 22 semanas (5.5 meses) solo Android
- **Requiere POCs obligatorios** — semana 1 para SignalR, semana 15 para WebRTC
- **Talento escaso** — pocos devs con experiencia KMP en producción

### Riesgos (ALTOS)
- POC de SignalR podría fallar → plan B sería cambiar de enfoque
- WebRTC wrapper complejo y frágil
- Debugging entre capas shared/platform es difícil

### Costo estimado: ~$85k Android, +$35k iOS después | Timeline: 22 semanas

---

## Tabla Comparativa

| Criterio | Kotlin Nativo | React Native | KMP |
|----------|:---:|:---:|:---:|
| **SignalR integración** | Oficial Java | Oficial JS (mismo que web) | No existe (wrapper custom) |
| **WebRTC madurez** | Excelente | Buena | Por piezas (expect/actual) |
| **UX nativa** | Perfecta | Buena (con esfuerzo) | Perfecta |
| **Performance** | Excelente | Buena | Excelente |
| **Time to market** | 18-23 sem | **16-18 sem** | 22 sem |
| **Reutilización iOS** | 0% | ~85% | ~50-65% |
| **Curva de aprendizaje** | Media | **Baja (si sabes TS)** | Alta |
| **Riesgo técnico** | Bajo | Bajo-Medio | **Alto** |
| **Offline support** | Room (excelente) | WatermelonDB (bueno) | SQLDelight (bueno) |
| **Push notifications** | FCM nativo | FCM via bridge | FCM nativo |
| **Mantenimiento** | 1 codebase | 1 codebase (2 platforms) | 2-3 codebases |
| **Tamaño APK** | ~15MB | ~25-30MB | ~15MB |
| **Hot reload** | Limited (Compose preview) | **Excelente** | Limited |

---

## Mi Recomendación

### Para tu caso específico, recomiendo: **Opción A (Kotlin Nativo)**

**Razones:**

1. **Dijiste "comenzar con Android"** — no necesitas cross-platform ahora
2. **SignalR y WebRTC son el corazón de la app** — necesitas las mejores implementaciones posibles, sin wrappers ni bridges
3. **UX premium sin compromisos** — Bubble notifications, PiP, Dynamic Colors, todo nativo
4. **Menor riesgo técnico** — todo funciona con librerías oficiales y probadas
5. **Tu backend es .NET con SignalR** — el cliente Java oficial es mantenido por Microsoft

### Si tu equipo ya domina TypeScript y quieres iOS pronto: **Opción B (React Native)**

Es la segunda mejor opción. La ventaja de usar el mismo `@microsoft/signalr` que ya usas en Angular es enorme — menos bugs, misma API, misma documentación.

### Descarto KMP para este proyecto

El riesgo de no tener cliente SignalR oficial es demasiado alto para una app de mensajería en tiempo real. El 50% de código compartido no justifica los riesgos.

---

## Preguntas para Decidir

1. **Tu equipo**: Kotlin/Android o TypeScript/React?
2. **iOS**: Lo necesitas en los próximos 6 meses?
3. **Prioridad**: Performance/UX premium vs velocidad de entrega?
4. **MVP o producto completo**: Quieres lanzar rápido un MVP o ir por todas las features?

---

## Documentos Detallados Generados

Los agentes crearon documentación extensa que puedes consultar:

- `ANDROID_APP_PLAN.md` — Plan detallado Kotlin Nativo (Opción A)
- `PLAN_ARQUITECTURA_MOBILE.md` — Plan detallado React Native (Opción B)
- `TAREAS_MOBILE.md` — Tareas React Native
- `RESUMEN_EJECUTIVO_MOBILE.md` — Resumen ejecutivo React Native
- `COMPARACION_FRAMEWORKS.md` — Comparación entre frameworks
- `PLAN_KOTLIN_MULTIPLATFORM.md` — Plan detallado KMP (Opción C)
- `PLAN_KMP_TAREAS.md` — Tareas KMP
- `KMP_ANALISIS_RIESGOS.md` — Análisis de riesgos KMP
- `KMP_RESUMEN_EJECUTIVO.md` — Resumen ejecutivo KMP
