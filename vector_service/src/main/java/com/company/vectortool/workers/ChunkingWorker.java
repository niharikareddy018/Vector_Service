package com.company.vectortool.workers;

import com.company.vectortool.config.RabbitMqConfig;
import com.company.vectortool.interfaces.ISqlDbAdaptor;
import com.company.vectortool.interfaces.IVectorDbAdaptor;
import com.company.vectortool.models.Document;
import com.company.vectortool.models.VectorEmbeddings;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChunkingWorker {

    private final ISqlDbAdaptor sqlDbAdaptor;
    private final IVectorDbAdaptor vectorDbAdaptor;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ChunkingWorker(ISqlDbAdaptor sqlDbAdaptor, IVectorDbAdaptor vectorDbAdaptor) {
        this.sqlDbAdaptor = sqlDbAdaptor;
        this.vectorDbAdaptor = vectorDbAdaptor;
    }

    @RabbitListener(queues = RabbitMqConfig.DOCUMENT_QUEUE)
    public void processDocumentTask(String payload) {
        try {
            String[] tokens = payload.split("\\|\\|");
            UUID documentId = UUID.fromString(tokens[0]);
            String documentName = tokens[1];
            String rawTextContent = tokens[2];

            List<String> chunks = new ArrayList<>();
            int chunkSize = 50;
            for (int i = 0; i < rawTextContent.length(); i += chunkSize) {
                chunks.add(rawTextContent.substring(i, Math.min(rawTextContent.length(), i + chunkSize)));
            }

            String textToEmbed = chunks.isEmpty() ? rawTextContent : chunks.get(0);
            String cleanText = textToEmbed.replace("\"", "\\\"");
            String jsonRequestBody = "{\"model\":\"nomic-embed-text\",\"prompt\":\"" + cleanText + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/embeddings"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            Pattern pattern = Pattern.compile("\\[([\\d.,\\sE-]+)\\]");
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                String rawVectorString = matcher.group(1);
                sqlDbAdaptor.createDocument(new Document(documentId, documentName));
                vectorDbAdaptor.storeEmbedding(new VectorEmbeddings(documentId, rawVectorString));
            }
        } catch (Exception e) {
            System.err.println("Java background worker processing failure: " + e.getMessage());
        }
    }
}
