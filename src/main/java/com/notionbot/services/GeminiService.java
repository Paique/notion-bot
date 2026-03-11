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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Consumer;

/**
 * Service for interacting with Google's Gemini AI.
 * Handles task refinement and date parsing with automatic retries.
 */
public class GeminiService {
    public record GeminiResult(String title, String dueDate, String description) {
    }

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final int MAX_RETRIES = 2;

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.apiKey = Config.getRequired("GEMINI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Synchronous wrapper for refineTextAsync.
     */
    public GeminiResult refineText(String rawText, String context) {
        try {
            return refineTextAsync(rawText, context).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to refine text synchronously: {}", e.getMessage());
            return new GeminiResult(rawText, null, null);
        }
    }

    /**
     * Refines a task description using Gemini AI.
     */
    public CompletableFuture<GeminiResult> refineTextAsync(String rawText, String contextText) {
        logger.info("Requesting Gemini to refine text: {}", rawText);
        CompletableFuture<GeminiResult> future = new CompletableFuture<>();

        String prompt = buildRefinePrompt(rawText, contextText);

        executeWithRetry(prompt, MAX_RETRIES,
                json -> {
                    String title = json.has("title") ? json.get("title").asText() : rawText;
                    String dueDate = getJsonString(json, "dueDate");
                    String description = getJsonString(json, "description");
                    future.complete(new GeminiResult(title, dueDate, description));
                },
                () -> future.complete(new GeminiResult(rawText, null, null)));

        return future;
    }

    /**
     * Parses a natural language date into ISO 8601.
     */
    public CompletableFuture<String> parseDateAsync(String dateInput) {
        logger.info("Parsing date input: {}", dateInput);
        CompletableFuture<String> future = new CompletableFuture<>();

        String prompt = buildDatePrompt(dateInput);

        executeWithRetry(prompt, MAX_RETRIES,
                json -> future.complete(getJsonString(json, "dueDate")),
                () -> future.complete(null));

        return future;
    }

    private void executeWithRetry(String prompt, int retriesLeft, Consumer<JsonNode> onSuccess,
            Runnable onFinalFailure) {
        try {
            String payload = buildPayload(prompt);
            HttpRequest request = buildRequest(payload);

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                String resultText = extractTextFromResponse(response.body());
                                JsonNode resultJson = objectMapper.readTree(cleanJsonMarkdown(resultText));
                                onSuccess.accept(resultJson);
                            } catch (Exception e) {
                                handleRetry(prompt, retriesLeft, onSuccess, onFinalFailure,
                                        "Parse error: " + e.getMessage());
                            }
                            return;
                        }
                        handleRetry(prompt, retriesLeft, onSuccess, onFinalFailure,
                                "API Error " + response.statusCode());
                    })
                    .exceptionally(ex -> {
                        handleRetry(prompt, retriesLeft, onSuccess, onFinalFailure,
                                "Network/Async error: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            handleRetry(prompt, retriesLeft, onSuccess, onFinalFailure, "Request preparation error: " + e.getMessage());
        }
    }

    private void handleRetry(String prompt, int retriesLeft, Consumer<JsonNode> onSuccess, Runnable onFinalFailure,
            String errorMsg) {
        logger.warn("{} - Retries left: {}", errorMsg, retriesLeft);
        if (retriesLeft > 0) {
            executeWithRetry(prompt, retriesLeft - 1, onSuccess, onFinalFailure);
            return;
        }
        onFinalFailure.run();
    }

    private String buildPayload(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("contents").addObject()
                .putArray("parts").addObject()
                .put("text", prompt);
        return objectMapper.writeValueAsString(root);
    }

    private HttpRequest buildRequest(String jsonPayload) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
    }

    private String extractTextFromResponse(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        return node.at("/candidates/0/content/parts/0/text").asText();
    }

    private String cleanJsonMarkdown(String text) {
        if (text == null)
            return "{}";
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String getJsonString(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private String buildRefinePrompt(String rawText, String contextText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Você é um assistente GTD eficiente. ");
        if (contextText != null && !contextText.isEmpty()) {
            sb.append("Contexto: ").append(contextText).append("\n\n");
        }
        sb.append(
                "Refine a entrada para uma tarefa acionável. Extraia título (sem data), calcule data real baseada em hoje (")
                .append(LocalDate.now(ZoneId.of("America/Sao_Paulo")))
                .append(") no formato ISO 8601, e uma breve descrição.\n\n")
                .append("Retorne APENAS um JSON:\n")
                .append("{\n")
                .append("  \"title\": \"Título da tarefa\",\n")
                .append("  \"dueDate\": \"YYYY-MM-DDTHH:mm:ss-03:00\",\n")
                .append("  \"description\": \"Instruções extras\"\n")
                .append("}\n\n")
                .append("Entrada: ").append(rawText);
        return sb.toString();
    }

    private String buildDatePrompt(String dateInput) {
        return "Converta para ISO 8601 sugerida baseada em hoje (" +
                LocalDate.now(ZoneId.of("America/Sao_Paulo")) +
                ") ou null se não houver data.\n" +
                "Retorne apenas JSON: {\"dueDate\": \"...\"}\n" +
                "Entrada: " + dateInput;
    }
}
