# Tareas de Desarrollo

## Fase 1: Configuración y Núcleo

- [x] Crear estructura de directorios (`src`, `tests`).
- [x] Crear archivos de planificación (`PLAN.md`, `TAREAS.md`).
- [ ] Crear `requirements.txt` con `mysql-connector-python` y `pytest`.
- [ ] Implementar el punto de entrada `src/main.py`.
- [ ] Implementar la solicitud de credenciales al usuario.
- [ ] Crear la clase `DBManager` en `src/db_manager.py` con un método `connect`.
- [ ] Implementar la conexión inicial en `main.py` usando `DBManager`.
- [ ] Crear el bucle REPL básico en `main.py`.

## Fase 2: Funcionalidades Básicas y Pruebas

- [ ] **Comando `SALIR`:**
    - [ ] Implementar lógica para cerrar la conexión y salir.
    - [ ] Escribir prueba.
- [ ] **Comando `CAMBIAR DB <nombre_db>`:**
    - [ ] Implementar método en `DBManager` para cambiar de base de datos.
    - [ ] Escribir prueba.
- [ ] **Comando `LISTAR TABLAS`:**
    - [ ] Implementar método en `DBManager` que ejecute `SHOW TABLES`.
    - [ ] Escribir prueba.

## Fase 3: Funcionalidades Avanzadas

- [ ] **Comando `DESCRIBIR <nombre_tabla>`:**
    - [ ] Implementar método en `DBManager` para obtener columnas, índices y relaciones de `INFORMATION_SCHEMA`.
    - [ ] Escribir prueba.
- [ ] **Comando `QUERY <consulta_sql>`:**
    - [ ] Implementar método genérico en `DBManager` para ejecutar consultas DML/DDL.
    - [ ] Diferenciar entre consultas que devuelven datos (`SELECT`) y las que no.
    - [ ] Escribir pruebas para `SELECT`, `INSERT`, `CREATE`.

## Fase 4: Backup y Restauración

- [ ] **Comando `BACKUP <archivo_salida>`:**
    - [ ] Implementar método en `DBManager` que llame a `mysqldump` con `subprocess`.
    - [ ] Escribir prueba (verificar que el comando se construye correctamente).
- [ ] **Comando `RESTAURAR <archivo_entrada>`:**
    - [ ] Implementar método en `DBManager` que llame a `mysql` con `subprocess`.
    - [ ] Escribir prueba.

## Fase 5: Refinamiento

- [ ] Mejorar el formato de salida de las tablas.
- [ ] Añadir manejo de errores robusto para comandos y conexión.
- [ ] Documentar el código.
