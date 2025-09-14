# Plan de Rectificación: mysql_cli_agent

## Objetivo

El objetivo de este plan es refactorizar el agente `mysql_cli_agent` para alinearlo con las directrices de implementación de un servidor MCP, utilizando `FastMCP` en lugar de `FastAPI`.

## Pasos

1.  **Detener el Servidor Actual**: El servidor FastAPI que se está ejecutando en segundo plano será detenido para evitar conflictos.

2.  **Actualizar Dependencias**: Se modificará el archivo `requirements.txt` para eliminar `fastapi` y `uvicorn`, y en su lugar, añadir `mcp[cli]`.

3.  **Instalar Nuevas Dependencias**: Se ejecutarán los comandos necesarios para instalar las nuevas dependencias en el entorno de desarrollo.

4.  **Refactorizar `mysql_mcp.py`**:
    *   Se reemplazará la implementación de `FastAPI` por `FastMCP`.
    *   Los endpoints HTTP serán convertidos a herramientas MCP utilizando el decorador `@mcp.tool()`.
    *   Las funciones de base de datos, que son síncronas, se adaptarán para funcionar correctamente en un entorno asíncrono.
    *   El punto de entrada del script se modificará para iniciar el servidor con `mcp.run(transport='stdio')`.

5.  **Verificación**: Se realizarán pruebas para asegurar que el nuevo servidor MCP funcione como se espera.
