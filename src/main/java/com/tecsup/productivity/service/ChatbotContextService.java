package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.response.*;
import com.tecsup.productivity.entity.*;
import com.tecsup.productivity.repository.*;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de contexto para el Chatbot
 * Proporciona información estructurada del usuario
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotContextService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final DailySummaryService dailySummaryService;
    private final SecurityUtil securityUtil;

    /**
     * Obtiene el contexto COMPLETO del usuario para el chatbot
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFullContext() {
        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();

        log.info("🤖 Generando contexto completo para chatbot - Usuario: {}", user.getEmail());

        Map<String, Object> context = new HashMap<>();

        // 1️⃣ Información del usuario
        context.put("user", buildUserInfo(user));

        // 2️⃣ Resumen del día actual
        context.put("today", buildTodayContext(user, today));

        // 3️⃣ Tareas próximas (7 días)
        context.put("upcoming", buildUpcomingContext(user, today));

        // 4️⃣ Tareas vencidas
        context.put("overdue", buildOverdueContext(user, today));

        // 5️⃣ Resumen de ayer
        context.put("yesterday", buildYesterdayContext(user, today.minusDays(1)));

        // 6️⃣ Estadísticas generales
        context.put("summary", buildSummaryStats(user, today));

        log.debug("✅ Contexto generado con {} secciones", context.size());
        return context;
    }

    /**
     * Genera un prompt formateado para Gemini con el contexto completo
     */
    @Transactional(readOnly = true)
    public String generateContextualPrompt(String userMessage) {
        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();

        StringBuilder prompt = new StringBuilder();

        // ============================================
        // IDENTIDAD DE LA IA
        // ============================================
        prompt.append("Eres ProductiBot, el asistente personal de ").append(user.getName()).append(".\n");
        prompt.append("Tu misión es ayudarle a ser más productivo y mantener buenos hábitos.\n\n");

        prompt.append("CAPACIDADES:\n");
        prompt.append("✅ Consultar tareas, eventos y hábitos\n");
        prompt.append("✅ Dar consejos personalizados de productividad\n");
        prompt.append("✅ Sugerir rutinas de ejercicio, estudio y bienestar\n");
        prompt.append("✅ Ayudar a organizar el tiempo y priorizar\n");
        prompt.append("✅ Motivar y dar seguimiento al progreso\n\n");

        prompt.append("ESTILO:\n");
        prompt.append("- Conciso (máximo 4-5 oraciones)\n");
        prompt.append("- Amigable y motivador 😊\n");
        prompt.append("- Usa emojis ocasionalmente (1-2 por respuesta)\n");
        prompt.append("- Práctico y accionable\n\n");

        // ============================================
        // FECHA ACTUAL
        // ============================================
        String fechaFormato = today.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy"));
        prompt.append("═══════════════════════════════════════\n");
        prompt.append("📅 HOY: ").append(fechaFormato).append("\n");
        prompt.append("═══════════════════════════════════════\n\n");

        // ============================================
        // TAREAS DEL DÍA
        // ============================================
        List<Task> tareasHoy = taskRepository.findByUserIdAndFechaLimite(user.getId(), today);
        long tareasPendientes = tareasHoy.stream().filter(t -> !t.getCompleted()).count();
        long tareasCompletadas = tareasHoy.stream().filter(Task::getCompleted).count();

        prompt.append("📋 TAREAS DE HOY: ").append(tareasHoy.size()).append(" total\n");
        if (!tareasHoy.isEmpty()) {
            prompt.append("   ✅ Completadas: ").append(tareasCompletadas).append("\n");
            prompt.append("   ⏳ Pendientes: ").append(tareasPendientes).append("\n\n");

            tareasHoy.stream()
                    .filter(t -> !t.getCompleted())
                    .limit(5)
                    .forEach(t -> {
                        prompt.append("   • ").append(t.getTitulo());
                        prompt.append(" [").append(t.getPrioridad()).append("]");
                        if (t.getSource().equals("tecsup")) {
                            prompt.append(" 🎓 TECSUP");
                        }
                        prompt.append("\n");
                    });
            prompt.append("\n");
        } else {
            prompt.append("   🎉 No hay tareas para hoy\n\n");
        }

        // ============================================
        // TAREAS VENCIDAS
        // ============================================
        List<Task> tareasVencidas = taskRepository.findOverdueTasks(user.getId(), today);
        if (!tareasVencidas.isEmpty()) {
            prompt.append("⚠️ TAREAS VENCIDAS: ").append(tareasVencidas.size()).append("\n");
            tareasVencidas.stream().limit(3).forEach(t -> {
                prompt.append("   • ").append(t.getTitulo());
                prompt.append(" (Venció: ").append(t.getFechaLimite()).append(")\n");
            });
            prompt.append("\n");
        }

        // ============================================
        // EVENTOS DEL DÍA
        // ============================================
        List<Event> eventosHoy = eventRepository.findByUserIdAndFecha(user.getId(), today);
        prompt.append("📅 EVENTOS DE HOY: ").append(eventosHoy.size()).append("\n");
        if (!eventosHoy.isEmpty()) {
            eventosHoy.forEach(e -> {
                prompt.append("   • ");
                if (e.getHora() != null) {
                    prompt.append(e.getHora()).append(" - ");
                }
                prompt.append(e.getTitulo());
                if (e.getCurso() != null && !e.getCurso().isEmpty()) {
                    prompt.append(" (").append(e.getCurso()).append(")");
                }
                prompt.append("\n");
            });
            prompt.append("\n");
        } else {
            prompt.append("   Sin eventos programados\n\n");
        }

        // ============================================
        // HÁBITOS DEL DÍA
        // ============================================
        List<Habit> habitos = habitRepository.findByUserIdAndActivoTrue(user.getId());
        List<HabitLog> logsHoy = habitLogRepository.findByUserAndDate(user.getId(), today);
        long habitosCompletados = logsHoy.stream().filter(HabitLog::getCompletado).count();

        prompt.append("💪 HÁBITOS: ").append(habitos.size()).append(" total\n");
        prompt.append("   ✅ Completados hoy: ").append(habitosCompletados).append("/").append(habitos.size()).append("\n\n");

        habitos.forEach(h -> {
            HabitLog log = logsHoy.stream()
                    .filter(l -> l.getHabit().getId().equals(h.getId()))
                    .findFirst()
                    .orElse(null);

            prompt.append("   ");
            if (log != null && log.getCompletado()) {
                prompt.append("✅");
            } else {
                prompt.append("⏳");
            }
            prompt.append(" ").append(h.getNombre());

            if (!h.getEsComida() && h.getMetaDiaria() != null) {
                int valor = (log != null && log.getValor() != null) ? log.getValor() : 0;
                prompt.append(" (").append(valor).append("/").append(h.getMetaDiaria()).append(")");
            }
            prompt.append("\n");
        });
        prompt.append("\n");

        // ============================================
        // PROGRESO DEL DÍA
        // ============================================
        DailySummary summary = dailySummaryService.calculateDailySummary(user, today);
        prompt.append("📊 PROGRESO DE HOY: ").append(summary.getProgressPercentage()).append("%\n");
        prompt.append("   Tareas: ").append(summary.getCompletedTasks()).append("/").append(summary.getTotalTasks()).append("\n");
        prompt.append("   Hábitos: ").append(summary.getCompletedHabits()).append("/").append(summary.getTotalHabits()).append("\n\n");

        // ============================================
        // TAREAS PRÓXIMAS (3 días)
        // ============================================
        LocalDate endDate = today.plusDays(3);
        List<Task> tareasProximas = taskRepository.findUpcomingTasks(user.getId(), today.plusDays(1), endDate);
        if (!tareasProximas.isEmpty()) {
            prompt.append("🔜 PRÓXIMAS TAREAS (3 días):\n");
            tareasProximas.stream().limit(5).forEach(t -> {
                prompt.append("   • ").append(t.getTitulo());
                prompt.append(" (").append(t.getFechaLimite()).append(")\n");
            });
            prompt.append("\n");
        }

        // ============================================
        // PREGUNTA DEL USUARIO
        // ============================================
        prompt.append("═══════════════════════════════════════\n");
        prompt.append("PREGUNTA:\n");
        prompt.append("\"").append(userMessage).append("\"\n");
        prompt.append("═══════════════════════════════════════\n\n");

        // ============================================
        // INSTRUCCIONES PARA LA IA
        // ============================================
        prompt.append("INSTRUCCIONES:\n");
        prompt.append("1. Responde basándote en la información arriba\n");
        prompt.append("2. Si preguntan sobre tareas/eventos, usa los datos exactos\n");
        prompt.append("3. Si piden consejos, sé específico y práctico\n");
        prompt.append("4. Menciona el progreso cuando sea relevante\n");
        prompt.append("5. Sé motivador pero realista\n");
        prompt.append("6. Si no hay info suficiente, sugiere alternativas\n");
        prompt.append("7. NO digas que puedes agendar cosas, solo consultar\n");

        log.debug("✅ Prompt contextual generado: {} caracteres", prompt.length());
        return prompt.toString();
    }

    // ============================================
    // MÉTODOS PRIVADOS - CONSTRUCCIÓN DE CONTEXTO
    // ============================================

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", user.getName());
        info.put("email", user.getEmail());
        info.put("tipo", user.getTipo());
        info.put("hasTecsupSync", user.getTecsupToken() != null);
        return info;
    }

    private Map<String, Object> buildTodayContext(User user, LocalDate today) {
        Map<String, Object> todayContext = new HashMap<>();
        todayContext.put("date", today);

        // Tareas
        List<Task> tasks = taskRepository.findByUserIdAndFechaLimite(user.getId(), today);
        todayContext.put("tasks", tasks.stream().map(this::mapTask).collect(Collectors.toList()));

        // Eventos
        List<Event> events = eventRepository.findByUserIdAndFecha(user.getId(), today);
        todayContext.put("events", events.stream().map(this::mapEvent).collect(Collectors.toList()));

        // Hábitos
        List<Habit> habits = habitRepository.findByUserIdAndActivoTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByUserAndDate(user.getId(), today);
        todayContext.put("habits", buildHabitsWithProgress(habits, logs));

        // Progreso
        DailySummary summary = dailySummaryService.calculateDailySummary(user, today);
        todayContext.put("progress", summary.getProgressPercentage());

        return todayContext;
    }

    private Map<String, Object> buildUpcomingContext(User user, LocalDate today) {
        LocalDate endDate = today.plusDays(7);
        List<Task> tasks = taskRepository.findUpcomingTasks(user.getId(), today.plusDays(1), endDate);

        Map<String, Object> upcoming = new HashMap<>();
        upcoming.put("tasks", tasks.stream().map(this::mapTask).collect(Collectors.toList()));
        upcoming.put("count", tasks.size());

        return upcoming;
    }

    private Map<String, Object> buildOverdueContext(User user, LocalDate today) {
        List<Task> tasks = taskRepository.findOverdueTasks(user.getId(), today);

        Map<String, Object> overdue = new HashMap<>();
        overdue.put("tasks", tasks.stream().map(this::mapTask).collect(Collectors.toList()));
        overdue.put("count", tasks.size());

        return overdue;
    }

    private Map<String, Object> buildYesterdayContext(User user, LocalDate yesterday) {
        DailySummary summary = dailySummaryService.getOrCalculateDailySummary(user, yesterday);

        Map<String, Object> yesterdayContext = new HashMap<>();
        yesterdayContext.put("date", yesterday);
        yesterdayContext.put("progress", summary.getProgressPercentage());
        yesterdayContext.put("totalTasks", summary.getTotalTasks());
        yesterdayContext.put("completedTasks", summary.getCompletedTasks());
        yesterdayContext.put("totalHabits", summary.getTotalHabits());
        yesterdayContext.put("completedHabits", summary.getCompletedHabits());

        return yesterdayContext;
    }

    private Map<String, Object> buildSummaryStats(User user, LocalDate today) {
        long totalTasks = taskRepository.countByUserId(user.getId());
        long completedTasks = taskRepository.countByUserIdAndCompleted(user.getId(), true);
        long overdueTasks = taskRepository.findOverdueTasks(user.getId(), today).size();
        long activeHabits = habitRepository.countByUserIdAndActivoTrue(user.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTasks", totalTasks);
        stats.put("completedTasks", completedTasks);
        stats.put("pendingTasks", totalTasks - completedTasks);
        stats.put("overdueTasks", overdueTasks);
        stats.put("activeHabits", activeHabits);

        return stats;
    }

    private List<Map<String, Object>> buildHabitsWithProgress(List<Habit> habits, List<HabitLog> logs) {
        return habits.stream().map(habit -> {
            HabitLog log = logs.stream()
                    .filter(l -> l.getHabit().getId().equals(habit.getId()))
                    .findFirst()
                    .orElse(null);

            Map<String, Object> habitMap = new HashMap<>();
            habitMap.put("id", habit.getId());
            habitMap.put("nombre", habit.getNombre());
            habitMap.put("tipo", habit.getTipo());
            habitMap.put("esComida", habit.getEsComida());
            habitMap.put("metaDiaria", habit.getMetaDiaria());
            habitMap.put("completado", log != null && log.getCompletado());
            habitMap.put("valorActual", log != null && log.getValor() != null ? log.getValor() : 0);

            return habitMap;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> mapTask(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("titulo", task.getTitulo());
        map.put("descripcion", task.getDescripcion());
        map.put("prioridad", task.getPrioridad());
        map.put("fechaLimite", task.getFechaLimite());
        map.put("completed", task.getCompleted());
        map.put("source", task.getSource());
        return map;
    }

    private Map<String, Object> mapEvent(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("titulo", event.getTitulo());
        map.put("fecha", event.getFecha());
        map.put("hora", event.getHora());
        map.put("categoria", event.getCategoria());
        map.put("curso", event.getCurso());
        return map;
    }
}