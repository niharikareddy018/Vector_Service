package com.company.vectortool.controllers;

import com.company.vectortool.config.RabbitMqConfig;
import com.company.vectortool.middleware.AuthService;
import com.company.vectortool.services.FileStorageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final RabbitTemplate rabbitTemplate;

    public GatewayController(AuthService authService, FileStorageService fileStorageService, RabbitTemplate rabbitTemplate) {
        this.authService = authService;
        this.fileStorageService = fileStorageService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> gatewayIngest(
            @RequestHeader("Authorization") String authToken,
            @RequestBody Map<String, String> requestBody) {
        
        if (!authService.validateToken(authToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Access Denied: Invalid Authentication Token.");
        }

        String filename = requestBody.get("filename");
        String rawTextContent = requestBody.get("rawTextContent");

        if (filename == null || rawTextContent == null) {
            return ResponseEntity.badRequest().body("Missing filename or rawTextContent parameters.");
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
        gatewayResponse.put("message", "File written to storage vault and pipeline processing task queued successfully.");

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
            // Fix: Aligned port routing runtime execution to port 8086 parameters
            ProcessBuilder processBuilder = new ProcessBuilder("python", "ai_agent.py", userPrompt);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder outputJson = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                outputJson.append(line);
            }
            process.waitFor();

            org.springframework.boot.json.JsonParser parser = org.springframework.boot.json.JsonParserFactory.getJsonParser();
            java.util.List<Object> pythonMatches = parser.parseList(outputJson.toString());

            Map<String, Object> structuredResult = new HashMap<>();
            structuredResult.put("queryStatus", "PROCESSED");
            structuredResult.put("authorizingRole", authService.getUserRole(authToken));
            structuredResult.put("retrievedContextMatches", pythonMatches);

            return ResponseEntity.ok(structuredResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Query Service runtime command execution failure: " + e.getMessage());
        }
    }
}
