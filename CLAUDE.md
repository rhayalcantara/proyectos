# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **MySQL Database Management MCP Server** built with Python and FastMCP. It exposes MySQL database operations as Model Context Protocol (MCP) tools that can be used by AI assistants like Claude to interact with MySQL databases.

**Note:** The codebase uses Spanish for variable names, comments, and docstrings.

## Common Commands

### Development Setup
```bash
# Install dependencies
pip install -r mysql_cli_agent/requirements.txt
```

### Testing
```bash
# Run all tests
pytest mysql_cli_agent/tests/

# Run tests with verbose output
pytest mysql_cli_agent/tests/ -v

# Run a specific test file
pytest mysql_cli_agent/tests/test_db_manager.py

# Run a specific test
pytest mysql_cli_agent/tests/test_db_manager.py::TestDBManager::test_execute_query_select_success
```

### Running the MCP Server
```bash
# Run the MCP server with stdio transport
python mysql_cli_agent/mysql_mcp.py
```

### System Requirements for Backup/Restore
The backup and restore functionality requires `mysqldump` and `mysql` command-line tools to be available in the system PATH.

## Architecture

### Layered Design

```
┌─────────────────────────────────┐
│   MCP Tools (mysql_mcp.py)      │  ← 7 async tools exposed via FastMCP decorators
├─────────────────────────────────┤
│   DBManager (db_manager.py)     │  ← Synchronous database operations
├─────────────────────────────────┤
│   mysql-connector-python         │  ← Database driver
└─────────────────────────────────┘
```

### Key Components

1. **`mysql_cli_agent/src/db_manager.py`** (206 lines)
   - Core `DBManager` class with synchronous methods
   - Manages connection lifecycle and state
   - Handles all MySQL operations: connect, close, list_tables, describe_table, execute_query, backup, restore
   - Uses `subprocess` to invoke `mysqldump` and `mysql` for backup/restore

2. **`mysql_cli_agent/mysql_mcp.py`** (78 lines)
   - FastMCP server entry point
   - Exposes 7 tools decorated with `@mcp.tool()`
   - Bridges async MCP interface to sync DBManager using `asyncio.to_thread()`
   - Maintains singleton `db_manager` instance

3. **`mysql_cli_agent/tests/test_db_manager.py`** (201 lines)
   - Comprehensive unit tests using unittest.mock
   - Mocks database connections and subprocess calls
   - No external database required for testing

### MCP Tools Exposed

All tools require active connection (checked before execution):

1. `database_connect(host, user, password, database, port=3306)` - Establish connection
2. `database_close_connection()` - Close active connection
3. `database_list_tables()` - Get table names
4. `database_describe_table(table_name)` - Get table schema
5. `database_execute_query(query)` - Execute any SQL (SELECT/INSERT/UPDATE/CREATE/etc.)
6. `database_backup(output_file)` - Create backup using mysqldump
7. `database_restore(input_file)` - Restore from SQL file (destructive operation)

### Critical Design Patterns

#### Connection State Management
- Only ONE active connection allowed at a time
- All tool operations validate connection state before executing
- Connection config stored in `db_manager.connection_config` for backup/restore operations
- Must explicitly close connection before connecting to different database

#### Async/Sync Bridge
The MCP tools are async but `DBManager` methods are synchronous. Bridge using:
```python
await asyncio.to_thread(db_manager.method, *args)
```

#### Error Handling Strategy
- **Connection errors**: Specific handling for access denied (ER_ACCESS_DENIED_ERROR) and bad database (ER_BAD_DB_ERROR)
- **Query errors**: Automatic rollback on SQL errors in `execute_query()`
- **Validation**: Table existence validated before describe operations
- **Subprocess errors**: Checks for missing mysqldump/mysql commands

#### Response Formats
- Success operations: `{'success': True/False, 'message': '...'}`
- Query results: `{'headers': [...], 'rows': [...]}`
- DML operations: `{'rows_affected': N}`
- Errors: `{'error': '...'}`

#### Query Execution
`execute_query()` handles both SELECT and DML/DDL operations:
- SELECT queries: Returns headers and rows
- INSERT/UPDATE/DELETE/CREATE: Commits transaction, returns rows affected
- Errors: Rolls back transaction

### Security Considerations

- Subprocess commands constructed as **lists**, not strings, to prevent injection
- Table names validated against `list_tables()` before use in queries
- Passwords passed directly to subprocess via command args (visible in process list)
- Restore operation validates file existence before execution

### Testing Strategy

Tests use `unittest.mock.MagicMock` to:
- Mock `mysql.connector.connect()` and connection objects
- Mock `subprocess.run()` for backup/restore operations
- Test both success paths and error scenarios
- Verify transaction control (commit/rollback)

No external MySQL database required for testing.

## Important Constraints

1. **Single Connection Limitation**: The server maintains only one database connection at a time. Must close before connecting elsewhere.

2. **Subprocess Dependency**: Backup and restore require `mysqldump` and `mysql` CLI tools in system PATH.

3. **No Connection Pooling**: Each session uses a single long-lived connection.

4. **Restore is Destructive**: The `database_restore()` operation will overwrite existing database content without confirmation.

## Recent Migration

The project was recently migrated from a planned FastAPI architecture to FastMCP (commit: "mcp mysql with backout"). This allows integration with MCP-compatible AI assistants instead of REST API clients.
