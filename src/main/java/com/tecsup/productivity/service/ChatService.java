// ============================================
// ChatService.java - EP-05 (HU-10, CA-15)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.entity.ChatMessage;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.repository.ChatMessageRepository;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        User user = securityUtil.getCurrentUser();

        // Verificar si el chat está habilitado
        Boolean chatEnabled = (Boolean) user.getPreferences().get("chatEnabled");
        if (chatEnabled == null || !chatEnabled) {
            throw new RuntimeException("El chatbot está deshabilitado");
        }

        // Generar respuesta basada en contexto
        String respuesta = generateResponse(user, request.getMensaje());

        // Guardar mensaje
        ChatMessage message = ChatMessage.builder()
                .user(user)
                .mensaje(request.getMensaje().trim())
                .respuesta(respuesta)
                .build();

        message = chatMessageRepository.save(message);

        return mapToChatMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Integer limit) {
        User user = securityUtil.getCurrentUser();

        List<ChatMessage> messages = chatMessageRepository.findRecentMessagesByUser(
                user.getId(),
                PageRequest.of(0, limit)
        );

        return messages.stream()
                .map(this::mapToChatMessageResponse)
                .collect(Collectors.toList());
    }

    // Generar respuesta del chatbot (SIMULADO - CA-15)
    private String generateResponse(User user, String mensaje) {
        String mensajeLower = mensaje.toLowerCase();

        // Respuestas basadas en palabras clave
        if (mensajeLower.contains("tarea") || mensajeLower.contains("pendiente")) {
            Long pendingCount = taskRepository.countPendingTasksByUser(user.getId());

            if (pendingCount == 0) {
                return "¡Excelente! No tienes tareas pendientes. 🎉";
            } else if (pendingCount == 1) {
                List<Task> tasks = taskRepository.findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(
                        user.getId(), false);
                Task task = tasks.get(0);
                return String.format("Tienes 1 tarea pendiente: '%s' (Prioridad: %s)",
                        task.getTitulo(), task.getPrioridad());
            } else {
                return String.format("Tienes %d tareas pendientes. Las de prioridad ALTA necesitan tu atención. 📝",
                        pendingCount);
            }
        }

        if (mensajeLower.contains("evento") || mensajeLower.contains("hoy") ||
                mensajeLower.contains("día")) {
            int eventCount = eventRepository.findByUserIdAndFechaOrderByHoraAsc(
                    user.getId(), LocalDate.now()).size();

            if (eventCount == 0) {
                return "No tienes eventos programados para hoy. 📅";
            } else if (eventCount == 1) {
                return "Tienes 1 evento programado para hoy. ¡Revisa tu calendario! 📅";
            } else {
                return String.format("Tienes %d eventos programados para hoy. ¡Revisa tu calendario! 📅",
                        eventCount);
            }
        }

        if (mensajeLower.contains("hola") || mensajeLower.contains("hi")) {
            return String.format("¡Hola %s! ¿En qué puedo ayudarte hoy? 😊", user.getName());
        }

        if (mensajeLower.contains("ayuda") || mensajeLower.contains("help")) {
            return "Puedo ayudarte con:\n" +
                    "• Ver tus tareas pendientes\n" +
                    "• Consultar eventos de hoy\n" +
                    "• Recordarte tus prioridades\n" +
                    "¡Solo pregúntame!";
        }

        // Respuesta por defecto
        return "Entiendo tu pregunta. Puedo ayudarte con tus tareas, eventos y hábitos. " +
                "Pregúntame sobre tus pendientes del día. 🤖";
    }

    private ChatMessageResponse mapToChatMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .userId(message.getUser().getId())
                .mensaje(message.getMensaje())
                .respuesta(message.getRespuesta())
                .createdAt(message.getCreatedAt())
                .build();
    }
}