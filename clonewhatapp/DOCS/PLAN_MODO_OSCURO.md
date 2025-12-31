# Plan: Modo Oscuro

## Objetivo
Implementar un tema oscuro alternativo para la aplicación, similar al de WhatsApp Web.

## Arquitectura

### Enfoque: CSS Variables + ThemeService
- Variables CSS en `:root` para tema claro (default)
- Variables CSS en `[data-theme="dark"]` para tema oscuro
- ThemeService en Angular para gestionar el estado
- Persistencia en localStorage (y opcionalmente en backend)

## Paleta de Colores

### Tema Claro (actual)
```css
--bg-primary: #ffffff
--bg-secondary: #f0f2f5
--bg-chat: #efeae2
--text-primary: #111b21
--text-secondary: #667781
--accent: #00a884
--border: #e9edef
--bubble-own: #d9fdd3
--bubble-other: #ffffff
```

### Tema Oscuro (WhatsApp style)
```css
--bg-primary: #111b21
--bg-secondary: #202c33
--bg-chat: #0b141a
--text-primary: #e9edef
--text-secondary: #8696a0
--accent: #00a884
--border: #222d34
--bubble-own: #005c4b
--bubble-other: #202c33
```

## Tareas

### T1: Variables CSS globales
- [ ] Crear archivo `_themes.scss` con variables CSS
- [ ] Definir tema claro como default
- [ ] Definir tema oscuro con selector `[data-theme="dark"]`

### T2: ThemeService
- [ ] Crear servicio para gestionar tema
- [ ] Guardar preferencia en localStorage
- [ ] Aplicar tema al cargar la app
- [ ] Detectar preferencia del sistema (prefers-color-scheme)

### T3: Toggle en UI
- [ ] Agregar botón de cambio de tema en sidebar header
- [ ] Icono de sol/luna según tema actual

### T4: Actualizar componentes
- [ ] Reemplazar colores hardcodeados por variables CSS
- [ ] Verificar contraste en modo oscuro
- [ ] Ajustar imágenes/iconos si es necesario

## Archivos a Modificar

1. `src/styles.scss` - Variables globales
2. Nuevo: `src/app/core/services/theme.service.ts`
3. `chat-layout.component.scss` - Sidebar styles
4. `chat-window.component.scss` - Chat styles
5. `chat-list.component.scss` - Lista de chats
6. `auth/*.scss` - Pantallas de login/registro

## Estimación
- 4 archivos principales a modificar
- ~200 líneas de CSS nuevas
- ~50 líneas de TypeScript (servicio)
