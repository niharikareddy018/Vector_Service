CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
    document_id UUID PRIMARY KEY,
    document_name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS vector_embeddings (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(document_id) ON DELETE CASCADE,
    embedding vector(768) NOT NULL
);

CREATE INDEX IF NOT EXISTS documents_hnsw_idx 
ON vector_embeddings USING hnsw (embedding vector_cosine_ops);
