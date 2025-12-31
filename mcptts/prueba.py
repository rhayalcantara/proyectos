import openai
import time

client = openai.OpenAI(
    base_url="http://127.0.0.1:8033/v1",
    api_key="sk-no-key-required"
)

try:
    inicio = time.time()
    token_count = 0
    
    stream = client.chat.completions.create(
        model="ggml-org/Qwen3-8B-GGUF:Q8_0",
        messages=[
            {"role": "system", "content": "You are a helpful AI assistant."},
            {"role": "user", "content": "Escribe una historia de navidad"}
        ],
        temperature=0.7,
        max_tokens=8000,
        stream=True
    )
    
    print("Generando respuesta...\n")
    full_response = ""
    
    for chunk in stream:
        if chunk.choices[0].delta.content:
            content = chunk.choices[0].delta.content
            print(content, end="", flush=True)
            full_response += content
            token_count += 1
    
    fin = time.time()
    tiempo_total = fin - inicio
    tokens_por_segundo = token_count / tiempo_total
    
    print(f"\n\n{'='*60}")
    print(f"🚀 Tokens generados: {token_count}")
    print(f"⏱️  Tiempo: {tiempo_total:.2f}s")
    print(f"⚡ Velocidad: {tokens_por_segundo:.2f} tokens/seg")
    print(f"{'='*60}")
    
except Exception as e:
    print(f"❌ Error: {e}")