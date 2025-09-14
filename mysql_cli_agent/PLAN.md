# Plan de Desarrollo: Agente CLI para MySQL

## 1. Objetivo

Crear una herramienta de línea de comandos (CLI) interactiva en Python para gestionar bases de datos MySQL. La herramienta permitirá a los usuarios conectarse a una base de datos, ejecutar consultas, realizar operaciones de DDL y DML, y gestionar copias de seguridad.

## 2. Arquitectura y Diseño

- **Lenguaje:** Python 3.
- **Interfaz:** Un bucle REPL (Read-Eval-Print Loop) que procesa comandos introducidos por el usuario.
- **Conexión a la Base de Datos:** Se utilizará la librería `mysql-connector-python`. La información de conexión se solicitará al inicio y se mantendrá durante la sesión.
- **Estructura del Proyecto:**
    - `src/`: Contendrá el código fuente principal.
        - `main.py`: Punto de entrada de la aplicación, maneja el bucle REPL.
        - `db_manager.py`: Clase `DBManager` que encapsula toda la lógica de interacción con la base de datos.
        - `commands.py`: Módulos para cada comando específico (listar tablas, describir, etc.).
    - `tests/`: Contendrá las pruebas unitarias y de integración.
    - `requirements.txt`: Lista de dependencias.
- **Manejo de Comandos:** El bucle principal en `main.py` parseará la entrada del usuario y delegará la acción a la función correspondiente en `DBManager`.
- **Backup/Restore:** Se invocarán los comandos de sistema `mysqldump` y `mysql` a través del módulo `subprocess` de Python.

## 3. Decisiones Técnicas

- **Framework de CLI:** No se usará un framework complejo como Click o Typer inicialmente para mantener la simplicidad. La lógica de comandos será un simple `if/elif/else` o un diccionario de mapeo.
- **Gestión de Dependencias:** `pip` y `requirements.txt`.
- **Framework de Pruebas:** `pytest`. Se mockeará la conexión a la base de datos para las pruebas unitarias siempre que sea posible.

## 4. Flujo de Usuario

1. El usuario ejecuta `python src/main.py`.
2. La aplicación solicita: Host, Usuario, Contraseña, Puerto y Base de Datos.
3. La aplicación intenta conectarse. Si tiene éxito, muestra un prompt `mysql-agent>`.
4. El usuario introduce comandos como `LISTAR TABLAS`, `DESCRIBIR mi_tabla`, `QUERY SELECT * FROM mi_tabla`, `BACKUP`, etc.
5. El comando `SALIR` cierra la conexión y termina el programa.
