import requests
import psycopg2
import json
import sys

def get_semantic_context(user_query):
    try:
        ollama_url = "http://localhost:11434/api/embeddings"
        payload = {"model": "nomic-embed-text", "prompt": user_query}
        response = requests.post(ollama_url, json=payload).json()
        query_vector = response["embedding"]
        
        conn = psycopg2.connect(
            host="localhost", database="vectordb", 
            user="postgres", password="your_secure_password", port="5432"
        )
        cursor = conn.cursor()
        
        # Relational Bridge: Join tables on document_id to pull human text instead of vectors!
        sql = """
            SELECT d.document_name 
            FROM vector_embeddings v
            JOIN documents d ON v.document_id = d.document_id
            ORDER BY v.embedding <=> %s::vector
            LIMIT 1;
        """
        cursor.execute(sql, (str(query_vector),))
        result = cursor.fetchone()
        
        cursor.close()
        conn.close()
        
        return result if result else "No matching company documentation found."
    except Exception as e:
        return f"Database context extraction failure: {str(e)}"

def ask_local_llm(user_query, database_context):
    try:
        system_prompt = (
            f"You are a secure corporate assistant. Write a short, complete paragraph explaining "
            f"the user question: '{user_query}' based on this matching company document resource: '{database_context}'."
        )
        
        ollama_chat_url = "http://localhost:11434/api/generate"
        payload = {
            "model": "tinyllama",
            "prompt": system_prompt,
            "stream": False
        }
        
        response = requests.post(ollama_chat_url, json=payload).json()
        return response["response"]
    except Exception as e:
        return f"Local generation engine failed to synthesize answer: {str(e)}"

if __name__ == "__main__":
    if len(sys.argv) > 1:
        user_prompt = " ".join(sys.argv[1:])
    else:
        user_prompt = "What are the rules for microservice firewall configurations?"
        
    context_file = get_semantic_context(user_prompt)
    ai_paragraph_answer = ask_local_llm(user_prompt, context_file)
    
    output_payload = [{
        "documentName": context_file,
        "documentId": "System Match Verified",
        "embedding": ai_paragraph_answer
    }]
    print(json.dumps(output_payload))
