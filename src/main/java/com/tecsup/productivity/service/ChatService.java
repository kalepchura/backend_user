package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Chat con IA (Gemini)
 * Ahora usa ChatbotContextService para contexto estructurado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiAIService geminiAIService;
    private final ChatbotContextService chatbotContextService;
    private final SecurityUtil securityUtil;

    /**
     * Envía un mensaje al chatbot con contexto completo del usuario
     */
    @Transactional(readOnly = true)
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        User user = securityUtil.getCurrentUser();

        log.info("💬 Usuario {} envía mensaje: {}", user.getEmail(), request.getMensaje());

        // Verificar si el chat está habilitado
        Boolean chatEnabled = (Boolean) user.getPreferences().getOrDefault("chatEnabled", true);
        if (!chatEnabled) {
            throw new RuntimeException("El chatbot está deshabilitado. Actívalo en tu configuración.");
        }

        // Verificar si Gemini está configurado
        if (!geminiAIService.isConfigured()) {
            log.error("❌ Gemini AI no está configurado");
            throw new RuntimeException("El servicio de IA no está disponible. Contacta al administrador.");
        }

        // Construir prompt con contexto completo
        String contextualPrompt = chatbotContextService.generateContextualPrompt(request.getMensaje());

        // Generar respuesta con Gemini
        String respuesta = geminiAIService.generateResponse(contextualPrompt);

        // Crear respuesta (NO se persiste en BD)
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(null) // No tiene ID porque NO se guarda
                .userId(user.getId())
                .mensaje(request.getMensaje().trim())
                .respuesta(respuesta)
                .createdAt(LocalDateTime.now())
                .build();

        // Agregar al historial temporal (caché)
        addToHistory(user.getId(), response);

        log.info("✅ Respuesta generada para usuario {}", user.getEmail());
        return response;
    }

    /**
     * Obtiene el historial temporal de la sesión
     */
    @Cacheable(value = "chatHistory", key = "#userId")
    public List<ChatMessageResponse> getSessionHistory(Long userId) {
        log.debug("📋 Inicializando historial en caché para usuario {}", userId);
        return new ArrayList<>();
    }

    /**
     * Agrega mensaje al historial temporal
     */
    private void addToHistory(Long userId, ChatMessageResponse message) {
        List<ChatMessageResponse> history = getSessionHistory(userId);
        history.add(message);

        // Limitar a últimos 20 mensajes
        if (history.size() > 20) {
            history.remove(0);
        }

        log.debug("📝 Historial actualizado. Total: {} mensajes", history.size());
    }

    /**
     * Limpia el historial de chat
     */
    @CacheEvict(value = "chatHistory", key = "#userId")
    public void clearHistory(Long userId) {
        log.info("🗑️ Historial limpiado para usuario {}", userId);
    }
}