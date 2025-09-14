import unittest
from unittest.mock import MagicMock
import sys
import os

# Añadir el directorio src al path para poder importar db_manager
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../src')))

from db_manager import DBManager

class TestDBManager(unittest.TestCase):
    """Pruebas para la clase DBManager."""

    def setUp(self):
        """Configura un mock de DBManager para cada prueba."""
        self.db_manager = DBManager()
        # Simulamos una conexión y un cursor exitosos
        self.db_manager.connection = MagicMock()
        self.db_manager.cursor = MagicMock()

    def test_list_tables_success(self):
        """Prueba que list_tables devuelve una lista de tablas si la consulta tiene éxito."""
        # Configura el mock del cursor para que devuelva un resultado simulado
        expected_tables_from_db = [('tabla1',), ('tabla2',)]
        self.db_manager.cursor.fetchall.return_value = expected_tables_from_db

        # Llama al método que queremos probar
        tables = self.db_manager.list_tables()

        # Verifica que el método se comportó como se esperaba
        self.db_manager.cursor.execute.assert_called_once_with("SHOW TABLES")
        self.assertEqual(tables, ['tabla1', 'tabla2'])

    def test_list_tables_empty(self):
        """Prueba que list_tables devuelve una lista vacía si no hay tablas."""
        # El cursor no devuelve ninguna fila
        self.db_manager.cursor.fetchall.return_value = []

        tables = self.db_manager.list_tables()

        self.db_manager.cursor.execute.assert_called_once_with("SHOW TABLES")
        self.assertEqual(tables, [])

    def test_describe_table_success(self):
        """Prueba que describe_table devuelve la estructura de una tabla existente."""
        # Simulamos la respuesta de SHOW COLUMNS y SHOW TABLES (para la validación)
        table_name = 'usuarios'
        self.db_manager.list_tables = MagicMock(return_value=[table_name]) # Simula que la tabla existe
        
        column_description = [
            ('id', 'int(11)', 'NO', 'PRI', None, 'auto_increment'),
            ('nombre', 'varchar(255)', 'YES', '', None, '')
        ]
        self.db_manager.cursor.fetchall.return_value = column_description
        self.db_manager.cursor.description = [('Field',), ('Type',), ('Null',), ('Key',), ('Default',), ('Extra',)]

        description = self.db_manager.describe_table(table_name)

        # Verificamos que se llamó a la consulta correcta
        self.db_manager.cursor.execute.assert_called_with(f"SHOW COLUMNS FROM {table_name}")
        
        # Verificamos que el resultado está bien estructurado
        expected_description = [
            {'Field': 'id', 'Type': 'int(11)', 'Null': 'NO', 'Key': 'PRI', 'Default': None, 'Extra': 'auto_increment'},
            {'Field': 'nombre', 'Type': 'varchar(255)', 'Null': 'YES', 'Key': '', 'Default': None, 'Extra': ''}
        ]
        self.assertEqual(description, expected_description)

    def test_describe_table_not_found(self):
        """Prueba que describe_table devuelve None si la tabla no existe."""
        table_name = 'tabla_inexistente'
        self.db_manager.list_tables = MagicMock(return_value=['otra_tabla']) # La tabla no está en la lista

        description = self.db_manager.describe_table(table_name)

        # Verificamos que no se hizo ninguna consulta de columnas
        self.db_manager.cursor.execute.assert_not_called()
        self.assertIsNone(description)

    def test_execute_query_select_success(self):
        """Prueba que execute_query con SELECT devuelve cabeceras y filas."""
        query = "SELECT id, nombre FROM usuarios"
        
        # Simulamos la respuesta del cursor
        self.db_manager.cursor.description = [('id',), ('nombre',)]
        self.db_manager.cursor.fetchall.return_value = [(1, 'Alice'), (2, 'Bob')]

        result = self.db_manager.execute_query(query)

        self.db_manager.cursor.execute.assert_called_once_with(query)
        expected_result = {
            'headers': ['id', 'nombre'],
            'rows': [(1, 'Alice'), (2, 'Bob')]
        }
        self.assertEqual(result, expected_result)

    def test_execute_query_insert_success(self):
        """Prueba que execute_query con INSERT devuelve el conteo de filas afectadas."""
        query = "INSERT INTO usuarios (nombre) VALUES ('Charlie')"
        
        # Simulamos la respuesta del cursor y la conexión
        self.db_manager.cursor.rowcount = 1

        result = self.db_manager.execute_query(query)

        self.db_manager.cursor.execute.assert_called_once_with(query)
        self.db_manager.connection.commit.assert_called_once()
        self.assertEqual(result, {'rows_affected': 1})

    def test_execute_query_sql_error(self):
        """Prueba que execute_query maneja errores de SQL y hace rollback."""
        query = "SELECT * FROM tabla_inexistente"
        
        # Simulamos un error de SQL
        import mysql.connector
        self.db_manager.cursor.execute.side_effect = mysql.connector.Error("La tabla no existe")

        result = self.db_manager.execute_query(query)

        self.db_manager.cursor.execute.assert_called_once_with(query)
        self.db_manager.connection.rollback.assert_called_once()
        self.assertIn("Error de SQL", result['error'])

    def test_backup_database_success(self):
        """Prueba que el comando de backup se construye y ejecuta correctamente."""
        # Configuramos los detalles de la conexión que se usarán para el backup
        self.db_manager.connection_config = {
            'host': 'localhost',
            'user': 'testuser',
            'password': 'testpass',
            'database': 'testdb',
            'port': 3306
        }
        output_file = 'backup.sql'

        # Mockeamos subprocess.run
        with unittest.mock.patch('subprocess.run') as mock_run:
            # Configuramos el mock para que simule una ejecución exitosa
            mock_run.return_value.returncode = 0
            mock_run.return_value.stderr = ''

            result = self.db_manager.backup_database(output_file)

            # Verificamos que el comando se construyó correctamente
            expected_command = [
                'mysqldump',
                '--host=localhost',
                '--port=3306',
                '--user=testuser',
                f"--password=testpass",
                'testdb',
                f"--result-file={output_file}"
            ]
            mock_run.assert_called_once_with(expected_command, capture_output=True, text=True)
            
            # Verificamos que el resultado es exitoso
            self.assertTrue(result['success'])

    def test_restore_database_success(self):
        """Prueba que el comando de restauración se construye y ejecuta correctamente."""
        self.db_manager.connection_config = {
            'host': 'localhost',
            'user': 'testuser',
            'password': 'testpass',
            'database': 'testdb',
            'port': 3306
        }
        input_file = 'backup.sql'

        # Mockeamos subprocess.run, open, y os.path.exists
        with unittest.mock.patch('subprocess.run') as mock_run, \
             unittest.mock.patch('builtins.open', unittest.mock.mock_open(read_data='SQL DUMP DATA')) as mock_file, \
             unittest.mock.patch('os.path.exists', return_value=True) as mock_exists:
            
            mock_run.return_value.returncode = 0
            mock_run.return_value.stderr = ''

            result = self.db_manager.restore_database(input_file)

            # Verificamos que se comprobó la existencia del archivo
            mock_exists.assert_called_once_with(input_file)

            # Verificamos que el archivo se intentó abrir
            mock_file.assert_called_once_with(input_file, 'r')

            # Verificamos que el comando se construyó correctamente
            expected_command = [
                'mysql',
                '--host=localhost',
                '--port=3306',
                '--user=testuser',
                f"--password=testpass",
                'testdb'
            ]
            # El handle del archivo mockeado se pasa como stdin
            mock_run.assert_called_once_with(expected_command, stdin=mock_file(), capture_output=True, text=True)
            
            self.assertTrue(result['success'])

if __name__ == '__main__':
    unittest.main()
