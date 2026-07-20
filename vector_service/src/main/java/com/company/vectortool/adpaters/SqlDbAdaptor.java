package com.company.vectortool.adpaters;

import com.company.vectortool.interfaces.ISqlDbAdaptor;
import com.company.vectortool.models.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SqlDbAdaptor implements ISqlDbAdaptor {
    private final JdbcTemplate jdbcTemplate;

    public SqlDbAdaptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Document> documentRowMapper = (rs, rowNum) -> new Document(
            UUID.fromString(rs.getString("document_id")),
            rs.getString("document_name")
    );

    @Override
    public void createDocument(Document document) {
        try {
            jdbcTemplate.update("INSERT INTO documents (document_id, document_name) VALUES (?, ?)", 
                document.getDocumentId(), document.getDocumentName());
        } catch (Exception e) {
            throw new RuntimeException("SQL creation trace error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Document> readDocument(UUID documentId) {
        try {
            return jdbcTemplate.query("SELECT document_id, document_name FROM documents WHERE document_id = ?", 
                documentRowMapper, documentId).stream().findFirst();
        } catch (Exception e) {
            throw new RuntimeException("SQL retrieval operational fault: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateDocument(Document document) {
        try {
            jdbcTemplate.update("UPDATE documents SET document_name = ? WHERE document_id = ?", 
                document.getDocumentName(), document.getDocumentId());
        } catch (Exception e) {
            throw new RuntimeException("SQL adjustment modification fault: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteDocument(UUID documentId) {
        try {
            jdbcTemplate.update("DELETE FROM documents WHERE document_id = ?", documentId);
        } catch (Exception e) {
            throw new RuntimeException("SQL deletion transaction exception: " + e.getMessage(), e);
        }
    }
}
