package com.company.vectortool.interfaces;

import com.company.vectortool.models.Document;
import java.util.Optional;
import java.util.UUID;

public interface ISqlDbAdaptor {
    void createDocument(Document document);
    Optional<Document> readDocument(UUID documentId);
    void updateDocument(Document document);
    void deleteDocument(UUID documentId);
}
