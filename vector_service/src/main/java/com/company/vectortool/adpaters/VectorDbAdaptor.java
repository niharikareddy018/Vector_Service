package com.company.vectortool.adpaters;

import com.company.vectortool.interfaces.IVectorDbAdaptor;
import com.company.vectortool.models.VectorEmbeddings;
import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public class VectorDbAdaptor implements IVectorDbAdaptor {
    private final JdbcTemplate jdbcTemplate;

    public VectorDbAdaptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void storeEmbedding(VectorEmbeddings embedding) {
        try {
            String[] parts = embedding.getEmbeddingString().split(",");
            float[] vectorData = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vectorData[i] = Float.parseFloat(parts[i].trim());
            }
            jdbcTemplate.update("INSERT INTO vector_embeddings (document_id, embedding) VALUES (?, ?)", 
                embedding.getDocumentId(), new PGvector(vectorData));
        } catch (Exception e) {
            throw new RuntimeException("pgvector serialization execution error: " + e.getMessage(), e);
        }
    }

    @Override
    public void purgeEmbedding(UUID documentId) {
        try {
            jdbcTemplate.update("DELETE FROM vector_embeddings WHERE document_id = ?", documentId);
        } catch (Exception e) {
            throw new RuntimeException("pgvector extraction drop error: " + e.getMessage(), e);
        }
    }
}
