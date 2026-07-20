package com.company.vectortool.controllers;

import com.company.vectortool.interfaces.ISqlDbAdaptor;
import com.company.vectortool.interfaces.IVectorDbAdaptor;
import com.company.vectortool.models.Document;
import com.company.vectortool.models.VectorEmbeddings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class VectorToolController {

    private final ISqlDbAdaptor sqlDbAdaptor;
    private final IVectorDbAdaptor vectorDbAdaptor;

    public VectorToolController(ISqlDbAdaptor sqlDbAdaptor, IVectorDbAdaptor vectorDbAdaptor) {
        this.sqlDbAdaptor = sqlDbAdaptor;
        this.vectorDbAdaptor = vectorDbAdaptor;
    }

    @PostMapping
    public ResponseEntity<String> createDocument(
            @RequestParam String embedding,
            @RequestParam UUID documentId,
            @RequestParam String documentName) {
        try {
            sqlDbAdaptor.createDocument(new Document(documentId, documentName));
            vectorDbAdaptor.storeEmbedding(new VectorEmbeddings(documentId, embedding));
            return ResponseEntity.ok("Document and vector embedding created successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error handling creation flow: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocument(
            @RequestParam UUID documentId,
            @RequestParam(required = false) String documentName) {
        try {
            return sqlDbAdaptor.readDocument(documentId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error executing retrieval flow: " + e.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<String> updateDocument(
            @RequestParam UUID documentId,
            @RequestParam String documentName) {
        try {
            sqlDbAdaptor.updateDocument(new Document(documentId, documentName));
            return ResponseEntity.ok("Document name modified successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error executing modification trace: " + e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<String> deleteDocument(
            @RequestParam UUID documentId,
            @RequestParam(required = false) String documentName) {
        try {
            vectorDbAdaptor.purgeEmbedding(documentId);
            sqlDbAdaptor.deleteDocument(documentId);
            return ResponseEntity.ok("Document assets permanently dropped.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error executing drop execution flow: " + e.getMessage());
        }
    }
}
