import requests
import json

SERVER_URL = "http://192.168.1.158:1234/v1"

def list_models():
    try:
        response = requests.get(f"{SERVER_URL}/models")
        response.raise_for_status() # Raise an exception for HTTP errors
        models = response.json()
        print("Available Models (Python):")
        for model in models.get("data", []):
            print(f"- {model.get('id')}")
    except requests.exceptions.RequestException as e:
        print(f"Error connecting to AI server (Python): {e}")
    except json.JSONDecodeError:
        print("Error: Could not decode JSON response (Python).")

if __name__ == "__main__":
    list_models()
