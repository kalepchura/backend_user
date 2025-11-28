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
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio mejorado para integración con Google Gemini AI
 * Con manejo de errores DNS, retry logic y fallback
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

    private WebClient webClient;

    @PostConstruct
    public void init() {
        // Inicializar WebClient una sola vez
        this.webClient = webClientBuilder.build();

        if (isConfigured()) {
            log.info("✅ Gemini API configurada correctamente");
            log.debug("📡 URL: {}", apiUrl);
            testConnection();
        } else {
            log.error("❌ Gemini API Key NO configurada o es inválida");
        }
    }

    /**
     * Test de conexión al iniciar
     */
    private void testConnection() {
        try {
            log.info("🔍 Probando conexión con Gemini...");
            String testResponse = generateResponse("test");
            if (testResponse != null && !testResponse.contains("problema")) {
                log.info("✅ Conexión con Gemini exitosa");
            }
        } catch (Exception e) {
            log.warn("⚠️ No se pudo conectar con Gemini en startup: {}", e.getMessage());
        }
    }

    /**
     * Verifica si la API está configurada correctamente
     */
    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isEmpty()
                && !apiKey.equals("TU_API_KEY_AQUI")
                && !apiKey.equals("YOUR_API_KEY_HERE")
                && apiKey.startsWith("AIza");
    }

    /**
     * Genera una respuesta usando Gemini AI con retry y fallback
     */
    public String generateResponse(String prompt) {
        if (!isConfigured()) {
            log.error("❌ Gemini no configurado");
            return getFallbackResponse(prompt);
        }

        try {
            log.debug("🤖 Generando respuesta (prompt: {} chars)", prompt.length());

            Map<String, Object> requestBody = buildRequestBody(prompt);

            String response = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("❌ Error HTTP {}: {}",
                                                clientResponse.statusCode(), body);
                                        return Mono.error(new RuntimeException(
                                                "Error API: " + clientResponse.statusCode()));
                                    })
                    )
                    .bodyToMono(String.class)

                    // RETRY LOGIC: 3 intentos con backoff exponencial
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter(throwable ->
                                    throwable instanceof WebClientRequestException ||
                                            throwable instanceof UnknownHostException)
                            .doBeforeRetry(retrySignal ->
                                    log.warn("⚠️ Reintento {}/3: {}",
                                            retrySignal.totalRetries() + 1,
                                            retrySignal.failure().getMessage())
                            )
                    )

                    // Timeout total de 60 segundos
                    .timeout(Duration.ofSeconds(60))

                    // Ejecutar de forma bloqueante
                    .block();

            log.debug("✅ Respuesta recibida de Gemini");
            return extractTextFromResponse(response);

        } catch (WebClientRequestException e) {
            if (e.getCause() instanceof UnknownHostException) {
                log.error("❌ ERROR DNS: No se puede resolver generativelanguage.googleapis.com");
                log.error("💡 Posibles causas: Sin internet, Firewall, DNS bloqueado");
                return getDNSErrorResponse();
            }
            log.error("❌ Error de red: {}", e.getMessage());
            return getNetworkErrorResponse();

        } catch (Exception e) {
            log.error("❌ Error inesperado al llamar Gemini: {}", e.getMessage(), e);
            return getFallbackResponse(prompt);
        }
    }

    /**
     * Construye el cuerpo de la petición para Gemini
     */
    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));

        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);

        requestBody.put("generationConfig", generationConfig);

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

            JsonNode promptFeedback = root.get("promptFeedback");
            if (promptFeedback != null) {
                JsonNode blockReason = promptFeedback.get("blockReason");
                if (blockReason != null) {
                    log.warn("⚠️ Bloqueado por seguridad: {}", blockReason.asText());
                    return "Lo siento, no puedo procesar esa solicitud por motivos de seguridad. 🔒";
                }
            }

            log.warn("⚠️ No se pudo extraer texto de respuesta");
            return "No pude generar una respuesta adecuada. ¿Podrías reformular tu pregunta? 🤔";

        } catch (Exception e) {
            log.error("❌ Error parseando respuesta: {}", e.getMessage());
            return "Hubo un error al procesar la respuesta. Por favor, intenta nuevamente. ⚠️";
        }
    }

    /**
     * Respuesta de fallback inteligente según el contexto
     */
    private String getFallbackResponse(String prompt) {
        String lower = prompt.toLowerCase();

        if (lower.contains("hoy") || lower.contains("qué tengo")) {
            return "📋 Por el momento no puedo conectarme al servicio de IA, pero puedes revisar tu dashboard para ver tus tareas y eventos de hoy.";
        }

        if (lower.contains("tarea") || lower.contains("task")) {
            return "✅ No puedo procesar tu pregunta ahora, pero puedes gestionar tus tareas directamente desde la sección de Tareas.";
        }

        if (lower.contains("evento") || lower.contains("event")) {
            return "📅 El servicio de IA no está disponible temporalmente. Revisa tu calendario para ver tus eventos.";
        }

        return "🤖 Lo siento, el servicio de IA no está disponible en este momento. Por favor, intenta más tarde o contacta al administrador.";
    }

    /**
     * Respuesta específica para errores DNS
     */
    private String getDNSErrorResponse() {
        return "🌐 No se puede conectar al servicio de IA debido a problemas de red.\n\n" +
                "💡 Posibles causas:\n" +
                "• Sin conexión a Internet\n" +
                "• Firewall bloqueando Google APIs\n" +
                "• Problemas de DNS\n\n" +
                "Por favor, contacta al administrador del sistema.";
    }

    /**
     * Respuesta para errores de red generales
     */
    private String getNetworkErrorResponse() {
        return "📡 Hay problemas de conexión con el servicio de IA.\n\n" +
                "Intenta nuevamente en unos momentos. Si el problema persiste, contacta al administrador.";
    }
}