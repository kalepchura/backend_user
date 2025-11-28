package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.service.ChatService;
import com.tecsup.productivity.service.ChatbotContextService;
import com.tecsup.productivity.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el Chatbot con IA
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatbotContextService chatbotContextService;
    private final SecurityUtil securityUtil;

    /**
     * POST /api/chat
     *
     * Enviar mensaje al chatbot
     * Body: { "mensaje": "¿Qué tengo hoy?" }
     *
     * Para: Chatbot flotante (enviar pregunta)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        log.info("💬 [POST] /api/chat - Mensaje: {}", request.getMensaje());

        ChatMessageResponse response = chatService.sendMessage(request);

        return ResponseEntity.ok(
                ApiResponse.success("Respuesta generada", response)
        );
    }

    /**
     * GET /api/chat/history
     *
     * Obtener historial temporal de la sesión
     * (No persiste en BD, solo en caché)
     *
     * Para: Mostrar conversación anterior en el chatbot
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getSessionHistory() {
        log.info("📋 [GET] /api/chat/history");

        Long userId = securityUtil.getCurrentUser().getId();
        List<ChatMessageResponse> history = chatService.getSessionHistory(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Historial obtenido", history)
        );
    }

    /**
     * DELETE /api/chat/history
     *
     * Limpiar historial temporal
     *
     * Para: Botón "Limpiar conversación" en chatbot
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {
        log.info("🗑️ [DELETE] /api/chat/history");

        Long userId = securityUtil.getCurrentUser().getId();
        chatService.clearHistory(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Historial limpiado exitosamente", null)
        );
    }

    /**
     * GET /api/chat/context
     *
     * Obtener contexto completo del usuario
     * (Información que el chatbot puede ver)
     *
     * Para: Debug o mostrar al usuario qué info tiene el bot
     */
    @GetMapping("/context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContext() {
        log.info("🔍 [GET] /api/chat/context");

        Map<String, Object> context = chatbotContextService.getFullContext();

        return ResponseEntity.ok(
                ApiResponse.success("Contexto del chatbot obtenido", context)
        );
    }

    /**
     * GET /api/chat/status
     *
     * Verificar estado del chatbot
     *
     * Para: Health check del servicio
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        log.info("✅ [GET] /api/chat/status");

        Map<String, Object> status = Map.of(
                "active", true,
                "service", "Gemini AI",
                "version", "1.0"
        );

        return ResponseEntity.ok(
                ApiResponse.success("Chatbot activo", status)
        );
    }
}