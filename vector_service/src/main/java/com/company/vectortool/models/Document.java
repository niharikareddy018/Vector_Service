package com.company.vectortool.models;

import java.util.UUID;

public class Document {
    private UUID documentId;
    private String documentName;

    public Document() {}
    public Document(UUID documentId, String documentName) {
        this.documentId = documentId;
        this.documentName = documentName;
    }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
}
