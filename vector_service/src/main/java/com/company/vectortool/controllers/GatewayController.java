package com.company.vectortool.controllers;

import com.company.vectortool.config.RabbitMqConfig;
import com.company.vectortool.middleware.AuthService;
import com.company.vectortool.services.FileStorageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    public GatewayController(AuthService authService, FileStorageService fileStorageService, RabbitTemplate rabbitTemplate) {
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> gatewayIngest(
            @RequestHeader("Authorization") String authToken,
            @RequestParam String filename,
            @RequestParam String rawTextContent) {
        
        if (!authService.validateToken(authToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Invalid Authentication Token.");
        }

        UUID documentId = UUID.randomUUID();
        byte[] rawBytes = rawTextContent.getBytes();
        String storagePathReference = fileStorageService.commitToStorage(documentId, filename, rawBytes);

        String messagePayload = documentId.toString() + "||" + filename + "||" + rawTextContent;
        rabbitTemplate.convertAndSend(RabbitMqConfig.DOCUMENT_QUEUE, messagePayload);

        Map<String, Object> gatewayResponse = new HashMap<>();
        gatewayResponse.put("status", "SUCCESS");
        gatewayResponse.put("userRole", authService.getUserRole(authToken));
        gatewayResponse.put("allocatedId", documentId);
        gatewayResponse.put("fileStorageVaultLocation", storagePathReference);
        gatewayResponse.put("message", "File written to storage vault and pipeline processing asynchronous task queued successfully.");

        return ResponseEntity.ok(gatewayResponse);
    }

    @GetMapping("/ask")
    public ResponseEntity<?> gatewayQuery(
            @RequestHeader("Authorization") String authToken,
            @RequestParam String userPrompt) {

        if (!authService.validateToken(authToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Invalid Authentication Token.");
        }

        try {
            // Updated port target path routing directly to 8087
            String pythonAgentUrl = "http://localhost:8087/api/documents/search?userPrompt=" + userPrompt;
            ResponseEntity<Object[]> agentResponse = restTemplate.getForEntity(pythonAgentUrl, Object[].class);
            
            Map<String, Object> structuredResult = new HashMap<>();
            structuredResult.put("queryStatus", "PROCESSED");
            structuredResult.put("authorizingRole", authService.getUserRole(authToken));
            structuredResult.put("retrievedContextMatches", agentResponse.getBody());

            return ResponseEntity.ok(structuredResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Query Service routing failure talking to downstream Python Agent: " + e.getMessage());
        }
    }
}
