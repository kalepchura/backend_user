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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final DailySummaryService dailySummaryService;
    private final SecurityUtil securityUtil;

    /**
     * Obtener resumen completo del día actual
     */
    @Transactional(readOnly = true)
    public DashboardResponse getTodayDashboard() {

        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();

        log.info("📊 Generando dashboard para: {} - {}", user.getEmail(), today);

        // 1️⃣ Tareas del día
        List<Task> tasks = taskRepository.findByUserIdAndFechaLimite(user.getId(), today);
        List<TaskResponse> taskResponses = tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        // 2️⃣ Eventos del día
        List<Event> events = eventRepository.findByUserIdAndFecha(user.getId(), today);
        List<EventResponse> eventResponses = events.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        // 3️⃣ Hábitos del día (con progreso)
        List<Habit> activeHabits = habitRepository.findByUserIdAndActivoTrue(user.getId());
        List<HabitWithProgressResponse> habitResponses = activeHabits.stream()
                .map(habit -> {
                    HabitLog log = habitLogRepository
                            .findByHabitIdAndFecha(habit.getId(), today)
                            .orElse(null);
                    return mapToHabitWithProgress(habit, log);
                })
                .collect(Collectors.toList());

        // 4️⃣ Tareas vencidas (no completadas)
        List<Task> overdueTasks = taskRepository.findOverdueTasks(user.getId(), today);
        List<TaskResponse> overdueResponses = overdueTasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        // 5️⃣ Calcular progreso del día
        DailySummary summary = dailySummaryService.calculateDailySummary(user, today);

        // 6️⃣ Construir respuesta
        return DashboardResponse.builder()
                .fecha(today)
                .tareas(taskResponses)
                .eventos(eventResponses)
                .habitos(habitResponses)
                .tareasVencidas(overdueResponses)
                .progresoDelDia(summary.getProgressPercentage())
                .totalTareas(summary.getTotalTasks())
                .tareasCompletadas(summary.getCompletedTasks())
                .totalHabitos(summary.getTotalHabits())
                .habitosCompletados(summary.getCompletedHabits())
                .build();
    }

    /**
     * Obtener tareas próximas (siguientes 7 días)
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getUpcomingTasks(int days) {

        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);

        List<Task> tasks = taskRepository.findUpcomingTasks(user.getId(), today, endDate);

        return tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener tareas vencidas
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks() {

        User user = securityUtil.getCurrentUser();
        List<Task> tasks = taskRepository.findOverdueTasks(user.getId(), LocalDate.now());

        return tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    // ============================================
    // MAPPERS
    // ============================================

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .titulo(task.getTitulo())
                .descripcion(task.getDescripcion())
                .prioridad(task.getPrioridad())
                .fechaLimite(task.getFechaLimite())
                .completed(task.getCompleted())
                .source(task.getSource())
                .tecsupExternalId(task.getTecsupExternalId())
                .sincronizadoTecsup(task.getSincronizadoTecsup())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .titulo(event.getTitulo())
                .fecha(event.getFecha())
                .hora(event.getHora())
                .categoria(event.getCategoria())
                .descripcion(event.getDescripcion())
                .curso(event.getCurso())
                .source(event.getSource())
                .tecsupExternalId(event.getTecsupExternalId())
                .sincronizadoTecsup(event.getSincronizadoTecsup())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    private HabitWithProgressResponse mapToHabitWithProgress(Habit habit, HabitLog log) {
        return HabitWithProgressResponse.builder()
                .id(habit.getId())
                .nombre(habit.getNombre())
                .tipo(habit.getTipo())
                .esComida(habit.getEsComida())
                .metaDiaria(habit.getMetaDiaria())
                .activo(habit.getActivo())
                .completado(log != null && log.getCompletado())
                .valorActual(log != null ? log.getValor() : 0)
                .progreso(calculateHabitProgress(habit, log))
                .build();
    }

    private Integer calculateHabitProgress(Habit habit, HabitLog log) {
        if (log == null || log.getValor() == null) {
            return 0;
        }

        if (habit.getEsComida()) {
            return log.getCompletado() ? 100 : 0;
        }

        if (habit.getMetaDiaria() == null || habit.getMetaDiaria() == 0) {
            return 0;
        }

        int progress = Math.round((log.getValor() * 100.0f) / habit.getMetaDiaria());
        return Math.min(progress, 100);
    }
}