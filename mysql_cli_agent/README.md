# Agente MCP para MySQL

Este proyecto implementa un agente de Protocolo de Contexto de Modelo (MCP) que proporciona un conjunto de herramientas para interactuar con una base de datos MySQL. Permite a un cliente MCP (como un asistente de IA) conectarse, consultar y gestionar una base de datos MySQL.

## Requisitos Previos

- Python 3.10 o superior
- `pip` para la gestión de paquetes

## Instalación

1. Clona este repositorio en tu máquina local.
2. Navega al directorio del proyecto.
3. Instala las dependencias necesarias ejecutando:

   ```bash
   pip install -r requirements.txt
   ```

## Configuración del Cliente MCP

Para utilizar este agente, debes configurar tu cliente MCP (por ejemplo, Claude for Desktop, Gemini CLI) para que sepa cómo iniciar el servidor. Añade el siguiente objeto JSON a la configuración de tu cliente (por ejemplo, en `claude_desktop_config.json` o `~/.gemini/settings.json`).

**Importante:** Asegúrate de reemplazar `/ruta/absoluta/a/tu/python3` y `/ruta/absoluta/a/tu/proyecto` con las rutas correctas en tu sistema.

```json
{
  "mcpServers": {
    "mysql_agent": {
      "command": "/ruta/absoluta/a/tu/python3",
      "args": [
        "/ruta/absoluta/a/tu/proyecto/mysql_cli_agent/mysql_mcp.py"
      ]
    }
  }
}
```

Puedes encontrar la ruta a tu ejecutable de Python 3 ejecutando `which python3` (en macOS/Linux) o `where python3` (en Windows).

## Uso

El servidor MCP es iniciado automáticamente por el cliente MCP cuando este se inicia, basándose en la configuración proporcionada. No es necesario ejecutar el script `mysql_mcp.py` manualmente.

Una vez configurado, puedes pedirle a tu asistente de IA que utilice las herramientas de la base de datos.

## Herramientas Disponibles

A continuación se muestra una lista de las herramientas que este agente pone a disposición del cliente MCP:

- `database_connect(host, user, password, database, port=3306)`: Se conecta a una base de datos MySQL.
- `database_close_connection()`: Cierra la conexión actual a la base de datos.
- `database_list_tables()`: Devuelve una lista con los nombres de las tablas de la base de datos actual.
- `database_describe_table(table_name)`: Devuelve la estructura (columnas) de una tabla específica.
- `database_execute_query(query)`: Ejecuta una consulta SQL (SELECT, INSERT, CREATE, etc.) en la base de datos.
- `database_backup(output_file)`: Crea un backup de la base de datos actual usando `mysqldump`.
- `database_restore(input_file)`: Restaura la base de datos desde un archivo `.sql`. **¡ADVERTENCIA: Operación destructiva!**
