package com.company.vectortool.controllers;

import com.company.vectortool.config.RabbitMqConfig;
import com.company.vectortool.interfaces.ISqlDbAdaptor;
import com.company.vectortool.interfaces.IVectorDbAdaptor;
import com.company.vectortool.models.Document;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class VectorToolController {

    private final ISqlDbAdaptor sqlDbAdaptor;
    private final IVectorDbAdaptor vectorDbAdaptor;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public VectorToolController(ISqlDbAdaptor sqlDbAdaptor, IVectorDbAdaptor vectorDbAdaptor, RabbitTemplate rabbitTemplate) {
        this.sqlDbAdaptor = sqlDbAdaptor;
        this.vectorDbAdaptor = vectorDbAdaptor;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping
    public ResponseEntity<String> createDocument(
            @RequestParam UUID documentId,
            @RequestParam String documentName,
            @RequestParam String rawTextContent) {
        try {
            String messagePayload = documentId.toString() + "||" + documentName + "||" + rawTextContent;
            rabbitTemplate.convertAndSend(RabbitMqConfig.DOCUMENT_QUEUE, messagePayload);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Document processing queued successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error queuing task payload: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/search")
    public ResponseEntity<?> semanticSearch(@RequestParam String userPrompt) {
        try {
            String url = "http://localhost:11434/api/embeddings";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "nomic-embed-text");
            requestBody.put("prompt", userPrompt);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getBody() == null || !response.getBody().containsKey("embedding")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Error: Ollama container returned an empty embedding array payload.");
            }

            List<Number> rawEmbedding = (List<Number>) response.getBody().get("embedding");
            if (rawEmbedding == null || rawEmbedding.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to parse vector array from Ollama model response.");
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rawEmbedding.size(); i++) {
                sb.append(rawEmbedding.get(i).doubleValue());
                if (i < rawEmbedding.size() - 1) sb.append(",");
            }

            List<UUID> matchingIds = vectorDbAdaptor.findSimilarDocuments(sb.toString(), 3);
            List<Map<String, Object>> searchResults = new ArrayList<>();

            for (UUID id : matchingIds) {
                sqlDbAdaptor.readDocument(id).ifPresent(doc -> {
                    Map<String, Object> resultRow = new HashMap<>();
                    resultRow.put("documentId", doc.getDocumentId());
                    resultRow.put("documentName", doc.getDocumentName());
                    resultRow.put("embedding", vectorDbAdaptor.getEmbeddingString(id));
                    searchResults.add(resultRow);
                });
            }
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error executing semantic search retrieval: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getDocument(@RequestParam UUID documentId) {
        try {
            return sqlDbAdaptor.readDocument(documentId).map(doc -> {
                String vectorData = vectorDbAdaptor.getEmbeddingString(documentId);
                Map<String, Object> combinedResponse = new HashMap<>();
                combinedResponse.put("documentId", doc.getDocumentId());
                combinedResponse.put("documentName", doc.getDocumentName());
                combinedResponse.put("embedding", vectorData != null ? vectorData : "No vector found");
                return ResponseEntity.ok(combinedResponse);
            }).orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<String> deleteDocument(@RequestParam UUID documentId) {
        try {
            vectorDbAdaptor.purgeEmbedding(documentId);
            sqlDbAdaptor.deleteDocument(documentId);
            return ResponseEntity.ok("Document assets permanently dropped.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error executing drop execution flow: " + e.getMessage());
        }
    }
}
