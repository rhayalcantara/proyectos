import requests
import json
import time

class OllamaMCP:
    def __init__(self, ollama_url="http://localhost:11434"):
        self.ollama_url = ollama_url
        self.current_model = None

    def _make_request(self, method, endpoint, data=None):
        url = f"{self.ollama_url}{endpoint}"
        try:
            if method == "GET":
                response = requests.get(url)
            elif method == "POST":
                response = requests.post(url, json=data)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"Error al conectar con Ollama: {e}")
            return None

    def list_models(self):
        response = self._make_request("GET", "/api/tags")
        if response and "models" in response:
            return [model["name"] for model in response["models"]]
        return []

    def load_model(self, model_name=None):
        models = self.list_models()
        if not models:
            print("No se encontraron modelos de Ollama disponibles.")
            self.current_model = None
            return False

        if model_name:
            if model_name in models:
                self.current_model = model_name
                print(f"Modelo '{model_name}' seleccionado.")
                return True
            else:
                print(f"El modelo '{model_name}' no está disponible. Modelos disponibles: {', '.join(models)}")
                self.current_model = None
                return False
        else:
            self.current_model = models[0]
            print(f"Ningún modelo especificado. Se cargó el primer modelo disponible: '{self.current_model}'.")
            return True

    def generate_response(self, messages):
        if not self.current_model:
            print("No hay un modelo cargado. Por favor, cargue un modelo primero.")
            return messages, 0

        data = {
            "model": self.current_model,
            "messages": messages,
            "stream": False
        }
        
        start_time = time.time()
        response = self._make_request("POST", "/api/chat", data=data)
        end_time = time.time()

        if response and "message" in response:
            llm_message = response["message"]
            messages.append(llm_message)
            
            # Calcular tokens por segundo (aproximado)
            # Ollama no devuelve directamente el número de tokens generados en /api/chat sin stream
            # Podríamos contar las palabras o caracteres como una aproximación
            # Para una estimación más precisa, necesitaríamos el recuento de tokens del LLM
            # Por ahora, usaremos una aproximación simple basada en la longitud del contenido
            
            content = llm_message.get("content", "")
            num_words = len(content.split())
            duration = end_time - start_time
            
            tokens_per_second = num_words / duration if duration > 0 else 0
            
            return messages, tokens_per_second
        
        return messages, 0

import sys

def main():
    mcp = OllamaMCP()

    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                break

            request = json.loads(line)
            tool_name = request.get("tool_name")
            inputs = request.get("inputs", {})
            request_id = request.get("id")

            response = {"mcp_protocol_version": "1.0", "id": request_id}
            payload = None
            error_payload = None

            if tool_name == "list_models":
                models = mcp.list_models()
                payload = {"models": models}
            elif tool_name == "load_model":
                model_name = inputs.get("model_name")
                success = mcp.load_model(model_name)
                if success:
                    payload = {"success": True, "loaded_model": mcp.current_model}
                else:
                    models = mcp.list_models()
                    error_payload = {
                        "message": f"El modelo '{model_name}' no está disponible.",
                        "available_models": models
                    }
            elif tool_name == "generate_response":
                messages = inputs.get("messages")
                if not mcp.current_model:
                    error_payload = {"message": "No hay un modelo cargado. Por favor, cargue un modelo primero."}
                elif not messages:
                    error_payload = {"message": "Faltan 'messages' en los inputs."}
                else:
                    updated_messages, tps = mcp.generate_response(messages)
                    payload = {"messages": updated_messages, "tokens_per_second": tps}
            else:
                error_payload = {"message": f"Herramienta desconocida: {tool_name}"}

            if payload is not None:
                response["payload"] = payload
            if error_payload is not None:
                response["error"] = error_payload

            sys.stdout.write(json.dumps(response) + '\n')
            sys.stdout.flush()

        except json.JSONDecodeError:
            response = {"error": {"message": "Invalid JSON input"}}
            sys.stdout.write(json.dumps(response) + '\n')
            sys.stdout.flush()
        except Exception as e:
            response = {"error": {"message": str(e)}}
            sys.stdout.write(json.dumps(response) + '\n')
            sys.stdout.flush()

if __name__ == "__main__":
    main()
