package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.Habit;
import com.tecsup.productivity.entity.HabitLog;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.HabitLogRepository;
import com.tecsup.productivity.repository.HabitRepository;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Chat con IA (Gemini)
 * Historial TEMPORAL en memoria (NO persiste en BD)
 * La IA puede ver TODO: tareas, eventos, hábitos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SecurityUtil securityUtil;
    private final GeminiAIService geminiAIService;

    @Value("${chatbot.context.max-tasks:10}")
    private int maxTasks;

    @Value("${chatbot.context.max-events:5}")
    private int maxEvents;

    /**
     * Envía un mensaje al chatbot y obtiene respuesta de la IA
     * El historial se mantiene en caché (sesión activa)
     */
    @Transactional(readOnly = true)
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        User user = securityUtil.getCurrentUser();

        log.info("Usuario {} envía mensaje al chatbot: {}", user.getEmail(), request.getMensaje());

        // Verificar si el chat está habilitado
        Boolean chatEnabled = (Boolean) user.getPreferences().getOrDefault("chatEnabled", true);
        if (!chatEnabled) {
            throw new RuntimeException("El chatbot está deshabilitado. Actívalo en tu configuración.");
        }

        // Verificar si Gemini está configurado
        if (!geminiAIService.isConfigured()) {
            log.error("Gemini AI no está configurado correctamente");
            throw new RuntimeException("El servicio de IA no está disponible. Contacta al administrador.");
        }

        // Construir contexto COMPLETO para la IA
        String contextualPrompt = buildContextualPrompt(user, request.getMensaje());

        // Generar respuesta con Gemini AI
        String respuesta = geminiAIService.generateResponse(contextualPrompt);

        // Crear respuesta (NO se guarda en BD)
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(null) // No tiene ID porque NO se persiste
                .userId(user.getId())
                .mensaje(request.getMensaje().trim())
                .respuesta(respuesta)
                .createdAt(LocalDateTime.now())
                .build();

        // Agregar al historial en caché (temporal)
        addToHistory(user.getId(), response);

        log.info("Respuesta generada exitosamente para usuario {}", user.getEmail());
        return response;
    }

    /**
     * Obtiene el historial temporal de la sesión actual
     * (almacenado en caché, NO en BD)
     */
    @Cacheable(value = "chatHistory", key = "#userId")
    public List<ChatMessageResponse> getSessionHistory(Long userId) {
        log.debug("Inicializando nuevo historial en caché para usuario {}", userId);
        return new ArrayList<>();
    }

    /**
     * Agrega un mensaje al historial temporal
     */
    private void addToHistory(Long userId, ChatMessageResponse message) {
        List<ChatMessageResponse> history = getSessionHistory(userId);
        history.add(message);

        // Limitar a últimos 20 mensajes para no saturar memoria
        if (history.size() > 20) {
            history.remove(0);
        }

        log.debug("Historial actualizado. Total mensajes: {}", history.size());
    }

    /**
     * Limpia el historial de chat de un usuario
     */
    @CacheEvict(value = "chatHistory", key = "#userId")
    public void clearHistory(Long userId) {
        log.info("Historial de chat limpiado para usuario {}", userId);
    }

    /**
     * Construye un prompt contextualizado COMPLETO para la IA
     * Incluye: Tareas, Eventos, Hábitos y progreso
     */
    private String buildContextualPrompt(User user, String mensaje) {
        StringBuilder prompt = new StringBuilder();

        // ============================================
        // IDENTIDAD Y PROPÓSITO DE LA IA
        // ============================================
        prompt.append("Eres ProductiBot, un asistente inteligente de productividad y bienestar.\n");
        prompt.append("Ayudas a ").append(user.getName()).append(" a gestionar su vida diaria.\n\n");

        prompt.append("CAPACIDADES:\n");
        prompt.append("- Consultar tareas, eventos y hábitos del usuario\n");
        prompt.append("- Dar consejos personalizados de productividad\n");
        prompt.append("- Sugerir rutinas de ejercicio, estudio y bienestar\n");
        prompt.append("- Ayudar a organizar el tiempo y priorizar actividades\n");
        prompt.append("- Recordar información importante del usuario\n\n");

        prompt.append("ESTILO DE RESPUESTA:\n");
        prompt.append("- Conciso (máximo 4-5 oraciones)\n");
        prompt.append("- Amigable y motivador 😊\n");
        prompt.append("- Práctico y accionable\n");
        prompt.append("- Usa emojis ocasionalmente (1-2 por respuesta)\n\n");

        // ============================================
        // CONTEXTO TEMPORAL
        // ============================================
        LocalDate hoy = LocalDate.now();
        LocalDate manana = hoy.plusDays(1);
        String hoyFormato = hoy.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy"));

        prompt.append("═══════════════════════════════════════\n");
        prompt.append("FECHA ACTUAL: ").append(hoyFormato).append("\n");
        prompt.append("═══════════════════════════════════════\n\n");

        // ============================================
        // TAREAS PENDIENTES
        // ============================================
        List<Task> tareasPendientes = taskRepository
                .findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(user.getId(), false)
                .stream()
                .limit(maxTasks)
                .toList();

        if (!tareasPendientes.isEmpty()) {
            prompt.append("📋 TAREAS PENDIENTES (").append(tareasPendientes.size()).append(" total):\n");
            tareasPendientes.forEach(t -> {
                prompt.append("  • ").append(t.getTitulo());
                prompt.append(" [").append(t.getPrioridad()).append("]");

                if (t.getFechaLimite() != null) {
                    if (t.getFechaLimite().isBefore(hoy)) {
                        prompt.append(" ⚠️ VENCIDA (").append(t.getFechaLimite()).append(")");
                    } else if (t.getFechaLimite().isEqual(hoy)) {
                        prompt.append(" 🔥 VENCE HOY");
                    } else if (t.getFechaLimite().isEqual(manana)) {
                        prompt.append(" ⏰ VENCE MAÑANA");
                    } else {
                        prompt.append(" (Vence: ").append(t.getFechaLimite()).append(")");
                    }
                }

                if (t.getDescripcion() != null && !t.getDescripcion().isEmpty()) {
                    prompt.append("\n    Descripción: ").append(t.getDescripcion());
                }

                prompt.append("\n");
            });
            prompt.append("\n");
        } else {
            prompt.append("📋 TAREAS PENDIENTES: ✅ Ninguna (¡Excelente!)\n\n");
        }

        // ============================================
        // EVENTOS DE HOY
        // ============================================
        List<Event> eventosHoy = eventRepository
                .findByUserIdAndFechaOrderByHoraAsc(user.getId(), hoy)
                .stream()
                .limit(maxEvents)
                .toList();

        if (!eventosHoy.isEmpty()) {
            prompt.append("📅 EVENTOS DE HOY:\n");
            eventosHoy.forEach(e -> {
                prompt.append("  • ").append(e.getHora() != null ? e.getHora() + " - " : "");
                prompt.append(e.getTitulo());
                prompt.append(" [").append(e.getCategoria()).append("]");

                if (e.getDescripcion() != null && !e.getDescripcion().isEmpty()) {
                    prompt.append("\n    Detalles: ").append(e.getDescripcion());
                }

                if (e.getCurso() != null && !e.getCurso().isEmpty()) {
                    prompt.append("\n    Curso: ").append(e.getCurso());
                }

                prompt.append("\n");
            });
            prompt.append("\n");
        } else {
            prompt.append("📅 EVENTOS DE HOY: Ninguno programado\n\n");
        }

        // ============================================
        // EVENTOS DE MAÑANA (Resumen)
        // ============================================
        List<Event> eventosManana = eventRepository
                .findByUserIdAndFechaOrderByHoraAsc(user.getId(), manana);

        if (!eventosManana.isEmpty()) {
            prompt.append("📆 EVENTOS DE MAÑANA (").append(eventosManana.size()).append("):\n");
            eventosManana.stream().limit(3).forEach(e -> {
                prompt.append("  • ").append(e.getHora() != null ? e.getHora() + " - " : "");
                prompt.append(e.getTitulo()).append("\n");
            });
            prompt.append("\n");
        }

        // ============================================
        // HÁBITOS Y PROGRESO
        // ============================================
        List<Habit> habitos = habitRepository.findByUserIdOrderByCreatedAtDesc(user.getId());


        if (!habitos.isEmpty()) {
            prompt.append("💪 HÁBITOS REGISTRADOS:\n");
            habitos.forEach(h -> {
                prompt.append("  • ").append(h.getNombre());
                prompt.append(" (").append(h.getTipo()).append(")");
                prompt.append(" - Meta: ").append(h.getMetaDiaria());

                // Buscar progreso de hoy
                habitLogRepository.findByHabitIdAndFecha(h.getId(), hoy)
                        .ifPresent(log -> {
                            if (log.getCompletado()) {
                                prompt.append(" ✅ COMPLETADO HOY");
                            } else {
                                prompt.append(" ⏳ Pendiente hoy");
                            }
                            if (log.getValor() != null) {
                                prompt.append(" (Valor: ").append(log.getValor()).append(")");
                            }
                        });

                prompt.append("\n");
            });
            prompt.append("\n");
        }

        // ============================================
        // PREGUNTA DEL USUARIO
        // ============================================
        prompt.append("═══════════════════════════════════════\n");
        prompt.append("PREGUNTA DEL USUARIO:\n");
        prompt.append("\"").append(mensaje).append("\"\n");
        prompt.append("═══════════════════════════════════════\n\n");

        // ============================================
        // INSTRUCCIONES FINALES PARA LA IA
        // ============================================
        prompt.append("INSTRUCCIONES:\n");
        prompt.append("1. Responde BASÁNDOTE en la información proporcionada arriba\n");
        prompt.append("2. Si preguntan sobre tareas/eventos específicos, usa los datos exactos\n");
        prompt.append("3. Si piden consejos (ejercicios, estudio, etc.), sé específico y práctico\n");
        prompt.append("4. Si preguntan sobre hábitos, menciona su progreso actual\n");
        prompt.append("5. Sé motivador y positivo, pero realista\n");
        prompt.append("6. Si no tienes información suficiente, sugiere consultar el calendario o tareas\n");
        prompt.append("7. Si te piden agendar algo, indica que el usuario puede hacerlo en su calendario\n");

        log.debug("Prompt construido con {} caracteres", prompt.length());
        return prompt.toString();
    }
}