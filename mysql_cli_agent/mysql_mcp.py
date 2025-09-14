import asyncio
import sys
import os
from mcp.server.fastmcp import FastMCP

# Añadir el directorio src al path para poder importar db_manager
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), 'src')))
from db_manager import DBManager

mcp = FastMCP("mysql_agent")
db_manager = DBManager()

@mcp.tool()
async def database_connect(host: str, user: str, password: str, database: str, port: int = 3306):
    """
    Se conecta a una base de datos MySQL.
    Almacena la configuración para futuras operaciones como backup.
    """
    if db_manager.connection and db_manager.connection.is_connected():
        return {"status": "Ya hay una conexión activa. Ciérrala primero si quieres conectar a otra base de datos."}
    
    config = {
        'host': host,
        'user': user,
        'password': password,
        'database': database,
        'port': port
    }
    success, message = await asyncio.to_thread(db_manager.connect, config)
    return {"success": success, "message": message}

@mcp.tool()
async def database_close_connection():
    """Cierra la conexión actual a la base de datos."""
    if not db_manager.connection or not db_manager.connection.is_connected():
        return {"status": "No hay ninguna conexión activa que cerrar."}
    await asyncio.to_thread(db_manager.close)
    return {"status": "Conexión cerrada exitosamente."}

@mcp.tool()
async def database_list_tables() -> list:
    """Devuelve una lista con los nombres de las tablas de la base de datos actual."""
    if not db_manager.connection or not db_manager.connection.is_connected():
        return {"error": "No estás conectado a ninguna base de datos. Usa database_connect primero."}
    return await asyncio.to_thread(db_manager.list_tables)

@mcp.tool()
async def database_describe_table(table_name: str) -> dict:
    """Devuelve la estructura (columnas) de una tabla específica."""
    if not db_manager.connection or not db_manager.connection.is_connected():
        return {"error": "No estás conectado a ninguna base de datos. Usa database_connect primero."}
    return await asyncio.to_thread(db_manager.describe_table, table_name)

@mcp.tool()
async def database_execute_query(query: str) -> dict:
    """Ejecuta una consulta SQL (SELECT, INSERT, CREATE, etc.) en la base de datos."""
    if not db_manager.connection or not db_manager.connection.is_connected():
        return {"error": "No estás conectado a ninguna base de datos. Usa database_connect primero."}
    return await asyncio.to_thread(db_manager.execute_query, query)

@mcp.tool()
async def database_backup(output_file: str) -> dict:
    """Crea un backup de la base de datos actual usando mysqldump."""
    if not db_manager.connection or not db_manager.connection.is_connected():
        return {"error": "No estás conectado a ninguna base de datos. Usa database_connect primero."}
    return await asyncio.to_thread(db_manager.backup_database, output_file)

@mcp.tool()
async def database_restore(input_file: str) -> dict:
    """
    Restaura la base de datos desde un archivo .sql. ¡ADVERTENCIA: Operación destructiva! 
    """
    if not db_manager.connection or not db_manager.connection.is_connected():
        return {"error": "No estás conectado a ninguna base de datos. Usa database_connect primero."}
    return await asyncio.to_thread(db_manager.restore_database, input_file)

if __name__ == "__main__":
    mcp.run(transport='stdio')