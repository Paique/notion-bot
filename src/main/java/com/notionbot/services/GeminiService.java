package com.notionbot.services;

import com.notionbot.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public GeminiService() {
        this.apiKey = Config.getRequired("GEMINI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String refineText(String rawText, String context) {
        try {
            return refineTextAsync(rawText, context).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to refine text synchronously: {}", e.getMessage());
            return rawText;
        }
    }

    public CompletableFuture<String> refineTextAsync(String rawText, String contextText) {
        logger.info("Requesting Gemini to refine text...");

        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode content = root.putArray("contents").addObject();

            StringBuilder prompt = new StringBuilder();
            prompt.append("Você é um assistente GTD altamente eficiente. ");
            if (contextText != null && !contextText.isEmpty()) {
                prompt.append("Use o seguinte contexto adicional (README/Markdown) para entender melhor a tarefa: \n\n")
                        .append(contextText)
                        .append("\n\n");
            }
            prompt.append("Refine a seguinte entrada para ser uma tarefa clara, física e acionável em português. ")
                    .append("Mantenha o título sucinto e direto. Entrada: ")
                    .append(rawText);

            content.putArray("parts").addObject().put("text", prompt.toString());

            String jsonPayload = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(response.body());
                                String result = jsonNode.at("/candidates/0/content/parts/0/text").asText(rawText)
                                        .trim();
                                future.complete(result);
                            } catch (Exception e) {
                                logger.error("Failed to parse Gemini response: {}", e.getMessage());
                                future.complete(rawText);
                            }
                        } else {
                            logger.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                            future.complete(rawText);
                        }
                        return null;
                    })
                    .exceptionally(ex -> {
                        logger.error("Async request failed: {}", ex.getMessage());
                        future.complete(rawText);
                        return null;
                    });

        } catch (Exception e) {
            logger.error("Failed to prepare Gemini request: {}", e.getMessage());
            future.complete(rawText);
        }

        return future;
    }
}
