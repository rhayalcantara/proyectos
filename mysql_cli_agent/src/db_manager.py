
import mysql.connector
from mysql.connector import errorcode

class DBManager:
    """
    Gestiona la conexión y las operaciones con la base de datos MySQL.
    """
    def __init__(self):
        self.connection_config = {}
        self.connection = None
        self.cursor = None

    def connect(self, config):
        """
        Establece una conexión con la base de datos.

        Args:
            config (dict): Un diccionario con 'host', 'user', 'password', 'port', 'database'.

        Returns:
            bool: True si la conexión fue exitosa, False en caso contrario.
            str: Un mensaje de éxito o error.
        """
        self.connection_config = config
        try:
            self.connection = mysql.connector.connect(**self.connection_config)
            self.cursor = self.connection.cursor()
            return True, "Conexión exitosa."
        except mysql.connector.Error as err:
            if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
                return False, "Error: Usuario o contraseña incorrectos."
            elif err.errno == errorcode.ER_BAD_DB_ERROR:
                return False, f"Error: La base de datos '{config.get('database')}' no existe."
            else:
                return False, f"Error al conectar: {err}"

    def close(self):
        """
        Cierra la conexión a la base de datos.
        """
        if self.connection and self.connection.is_connected():
            self.cursor.close()
            self.connection.close()
            print("Conexión cerrada.")

    def list_tables(self):
        """
        Obtiene la lista de tablas de la base de datos actual.

        Returns:
            list: Una lista de nombres de tablas.
        """
        try:
            self.cursor.execute("SHOW TABLES")
            # El resultado de fetchall es una lista de tuplas, ej: [('tabla1',), ('tabla2',)]
            # Lo convertimos a una lista de strings: ['tabla1', 'tabla2']
            tables = [table[0] for table in self.cursor.fetchall()]
            return tables
        except mysql.connector.Error as err:
            print(f"Error al listar tablas: {err}")
            return [] # Devuelve lista vacía en caso de error

    def describe_table(self, table_name):
        """
        Obtiene la descripción de las columnas de una tabla.

        Args:
            table_name (str): El nombre de la tabla a describir.

        Returns:
            list: Una lista de diccionarios, donde cada diccionario representa una columna.
                  Devuelve None si la tabla no existe.
        """
        # Medida de seguridad: Validar que la tabla existe antes de usarla en una consulta.
        if table_name not in self.list_tables():
            print(f"Error: La tabla '{table_name}' no existe.")
            return None

        try:
            query = f"SHOW COLUMNS FROM {table_name}"
            self.cursor.execute(query)
            
            # Obtenemos los nombres de las columnas de la descripción del cursor
            column_names = [desc[0] for desc in self.cursor.description]
            
            # Creamos una lista de diccionarios para un resultado más manejable
            table_description = []
            for row in self.cursor.fetchall():
                table_description.append(dict(zip(column_names, row)))
            
            return table_description
        except mysql.connector.Error as err:
            print(f"Error al describir la tabla: {err}")
            return None

    def execute_query(self, query):
        """
        Ejecuta una consulta SQL genérica.

        Args:
            query (str): La consulta SQL a ejecutar.

        Returns:
            dict: Un diccionario con los resultados o un mensaje de error.
                  Para SELECT: {'headers': [...], 'rows': [...]}
                  Para DML/DDL: {'rows_affected': N}
                  Para errores: {'error': "..."}
        """
        try:
            self.cursor.execute(query)
            
            # Determinar el tipo de consulta
            is_select_query = query.strip().upper().startswith('SELECT')
            
            if is_select_query:
                headers = [desc[0] for desc in self.cursor.description]
                rows = self.cursor.fetchall()
                return {'headers': headers, 'rows': rows}
            else:
                # Para INSERT, UPDATE, DELETE, CREATE, etc.
                self.connection.commit()
                return {'rows_affected': self.cursor.rowcount}

        except mysql.connector.Error as err:
            self.connection.rollback()
            return {'error': f"Error de SQL: {err}"}

    def backup_database(self, output_file):
        """
        Realiza una copia de seguridad de la base de datos usando mysqldump.

        Args:
            output_file (str): La ruta del archivo .sql donde se guardará el backup.

        Returns:
            dict: Un diccionario indicando el éxito o fracaso de la operación.
        """
        import subprocess

        config = self.connection_config
        try:
            # Construir el comando de forma segura como una lista
            command = [
                'mysqldump',
                f"--host={config['host']}",
                f"--port={config['port']}",
                f"--user={config['user']}",
                f"--password={config['password']}",
                config['database'],
                f"--result-file={output_file}"
            ]

            # Ejecutar el comando
            result = subprocess.run(command, capture_output=True, text=True)

            if result.returncode == 0:
                return {'success': True, 'message': f"Backup creado exitosamente en {output_file}"}
            else:
                # Intentar encontrar mysqldump en una ubicación común de macOS si no está en el PATH
                if "command not found" in result.stderr:
                    return {'success': False, 'error': "Error: `mysqldump` no encontrado en el PATH del sistema."}
                return {'success': False, 'error': f"Error durante el backup: {result.stderr}"}
        
        except Exception as e:
            return {'success': False, 'error': f"Ocurrió una excepción: {e}"}

    def restore_database(self, input_file):
        """
        Restaura una base de datos desde un archivo .sql usando el cliente mysql.

        Args:
            input_file (str): La ruta del archivo .sql para restaurar.

        Returns:
            dict: Un diccionario indicando el éxito o fracaso de la operación.
        """
        import subprocess
        import os

        if not os.path.exists(input_file):
            return {'success': False, 'error': f"El archivo de entrada '{input_file}' no existe."}

        config = self.connection_config
        try:
            command = [
                'mysql',
                f"--host={config['host']}",
                f"--port={config['port']}",
                f"--user={config['user']}",
                f"--password={config['password']}",
                config['database']
            ]

            with open(input_file, 'r') as f:
                result = subprocess.run(command, stdin=f, capture_output=True, text=True)

            if result.returncode == 0:
                return {'success': True, 'message': f"Restauración desde {input_file} completada exitosamente."}
            else:
                if "command not found" in result.stderr:
                     return {'success': False, 'error': "Error: `mysql` no encontrado en el PATH del sistema."}
                return {'success': False, 'error': f"Error durante la restauración: {result.stderr}"}

        except Exception as e:
            return {'success': False, 'error': f"Ocurrió una excepción: {e}"}
