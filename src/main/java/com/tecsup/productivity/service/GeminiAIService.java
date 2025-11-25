package com.tecsup.productivity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para integración con Google Gemini AI
 * Documentación: https://ai.google.dev/gemini-api/docs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiAIService {

    @Value("${google.gemini.api.key}")
    private String apiKey;

    @Value("${google.gemini.api.url}")
    private String apiUrl;

    @Value("${google.gemini.api.max-tokens:500}")
    private int maxTokens;

    @Value("${google.gemini.api.temperature:0.7}")
    private double temperature;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            log.info("✅ Gemini API Key configurada correctamente");
            log.debug("API URL: {}", apiUrl);
        } else {
            log.error("❌ Gemini API Key NO configurada o es inválida");
        }
    }

    /**
     * Verifica si la API está configurada correctamente
     */
    public boolean isConfigured() {
        // ✅ CORRECTO: Rechazar solo placeholders conocidos
        return apiKey != null
                && !apiKey.isEmpty()
                && !apiKey.equals("TU_API_KEY_AQUI")
                && !apiKey.equals("YOUR_API_KEY_HERE")
                && apiKey.startsWith("AIza"); // Las keys de Gemini siempre empiezan con "AIza"
    }

    /**
     * Genera una respuesta usando Gemini AI
     */
    public String generateResponse(String prompt) {
        try {
            log.debug("Generando respuesta con Gemini AI para prompt de {} caracteres", prompt.length());

            Map<String, Object> requestBody = buildRequestBody(prompt);

            WebClient webClient = webClientBuilder.build();
            String response = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> {
                                log.error("Error en llamada a Gemini API: {}", clientResponse.statusCode());
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            log.error("Cuerpo del error: {}", body);
                                            return Mono.error(new RuntimeException("Error al llamar a Gemini API: " + body));
                                        });
                            }
                    )
                    .bodyToMono(String.class)
                    .block();

            log.debug("✅ Respuesta recibida de Gemini AI exitosamente");
            return extractTextFromResponse(response);

        } catch (Exception e) {
            log.error("Error al llamar a Gemini API: {}", e.getMessage(), e);
            return "Lo siento, tuve un problema al procesar tu solicitud. Por favor, intenta de nuevo en unos momentos. 🤖";
        }
    }

    /**
     * Construye el cuerpo de la petición para Gemini
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        // Contenido del mensaje
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        requestBody.put("contents", List.of(content));

        // Configuración de generación
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);

        requestBody.put("generationConfig", generationConfig);

        // Configuraciones de seguridad
        List<Map<String, Object>> safetySettings = List.of(
                Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE"),
                Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE")
        );
        requestBody.put("safetySettings", safetySettings);

        return requestBody;
    }

    /**
     * Extrae el texto de la respuesta JSON de Gemini
     */
    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // Navegar por la estructura de respuesta de Gemini
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && parts.size() > 0) {
                        JsonNode text = parts.get(0).get("text");
                        if (text != null) {
                            return text.asText().trim();
                        }
                    }
                }
            }

            // Verificar si hubo un bloqueo de seguridad
            JsonNode promptFeedback = root.get("promptFeedback");
            if (promptFeedback != null) {
                JsonNode blockReason = promptFeedback.get("blockReason");
                if (blockReason != null) {
                    log.warn("Respuesta bloqueada por seguridad: {}", blockReason.asText());
                    return "Lo siento, no puedo procesar esa solicitud por motivos de seguridad.";
                }
            }

            log.warn("No se pudo extraer texto de la respuesta de Gemini: {}", jsonResponse);
            return "Lo siento, no pude generar una respuesta adecuada. ¿Podrías reformular tu pregunta?";

        } catch (Exception e) {
            log.error("Error al parsear respuesta de Gemini: {}", e.getMessage());
            return "Hubo un error al procesar la respuesta. Por favor, intenta nuevamente.";
        }
    }
}