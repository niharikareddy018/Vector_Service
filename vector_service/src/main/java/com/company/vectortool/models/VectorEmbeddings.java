package com.company.vectortool.models;

import java.util.UUID;

public class VectorEmbeddings {
    private UUID documentId;
    private String embeddingString;

    public VectorEmbeddings() {}
    public VectorEmbeddings(UUID documentId, String embeddingString) {
        this.documentId = documentId;
        this.embeddingString = embeddingString;
    }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getEmbeddingString() { return embeddingString; }
    public void setEmbeddingString(String embeddingString) { this.embeddingString = embeddingString; }
}
