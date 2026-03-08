package com.notionbot.services;

import com.notionbot.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";

    public GeminiService() {
        this.apiKey = Config.getRequired("GEMINI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String refineText(String rawText) {
        try {
            logger.info("Requesting Gemini to refine text...");

            ObjectNode root = objectMapper.createObjectNode();
            ObjectNode content = root.putArray("contents").addObject();
            content.putArray("parts").addObject().put("text",
                    "Você é um assistente GTD. Refine a seguinte entrada curta para ser uma tarefa clara, " +
                            "física e acionável. Mantenha sucinto e direto. Entrada: " + rawText);

            String jsonPayload = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var jsonNode = objectMapper.readTree(response.body());
                return jsonNode.at("/candidates/0/content/parts/0/text").asText(rawText).trim();
            } else {
                logger.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                return rawText;
            }
        } catch (Exception e) {
            logger.error("Failed to refine text with Gemini: {}", e.getMessage());
            return rawText;
        }
    }
}
