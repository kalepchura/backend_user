package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.service.ChatService;
import com.tecsup.productivity.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el Chatbot con IA
 * EP-05: HU-10 (Interacción con chatbot IA), CA-15
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SecurityUtil securityUtil;

    /**
     * Envía un mensaje al chatbot y recibe una respuesta generada por IA
     *
     * POST /api/chat
     * Body: { "mensaje": "¿Cuáles son mis tareas pendientes?" }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        log.info("POST /api/chat - Enviando mensaje al chatbot");
        ChatMessageResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Obtiene el historial temporal de la sesión actual
     * (No persiste en BD, solo en caché durante la sesión)
     *
     * GET /api/chat/history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getSessionHistory() {
        log.info("GET /api/chat/history - Obteniendo historial temporal");
        Long userId = securityUtil.getCurrentUser().getId();
        List<ChatMessageResponse> history = chatService.getSessionHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Limpia el historial temporal del chat
     *
     * DELETE /api/chat/history
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<String>> clearHistory() {
        log.info("DELETE /api/chat/history - Limpiando historial");
        Long userId = securityUtil.getCurrentUser().getId();
        chatService.clearHistory(userId);
        return ResponseEntity.ok(ApiResponse.success("Historial limpiado exitosamente"));
    }

    /**
     * Verifica el estado del chatbot
     *
     * GET /api/chat/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success("Chatbot activo y funcionando"));
    }
}