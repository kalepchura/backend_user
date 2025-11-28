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
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final DailySummaryService dailySummaryService;
    private final SecurityUtil securityUtil;

    /**
     * Obtener vista del mes completo
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthView(int year, int month) {

        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();

        log.info("📅 Generando vista de calendario: {}-{} para {}", year, month, user.getEmail());

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Obtener todas las actividades del mes
        List<Task> tasks = taskRepository.findByUserIdAndFechaLimiteBetween(
                user.getId(), startDate, endDate
        );
        List<Event> events = eventRepository.findByUserIdAndFechaBetween(
                user.getId(), startDate, endDate
        );

        // Obtener resúmenes guardados (días pasados)
        List<DailySummary> summaries = dailySummaryService.getMonthlySummaries(user, year, month);
        Map<LocalDate, DailySummary> summaryMap = summaries.stream()
                .collect(Collectors.toMap(DailySummary::getDate, s -> s));

        // Construir mapa de días
        Map<LocalDate, Map<String, Object>> daysMap = new HashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate currentDate = date;

            // Contar actividades del día
            long taskCount = tasks.stream()
                    .filter(t -> t.getFechaLimite().equals(currentDate))
                    .count();

            long eventCount = events.stream()
                    .filter(e -> e.getFecha().equals(currentDate))
                    .count();

            boolean hasActivity = taskCount > 0 || eventCount > 0;

            // Obtener progreso (de resumen o calculado)
            Integer progress = null;
            if (currentDate.isBefore(today)) {
                // Día pasado: buscar en resúmenes guardados
                DailySummary summary = summaryMap.get(currentDate);
                if (summary != null) {
                    progress = summary.getProgressPercentage();
                } else if (hasActivity) {
                    // Si hay actividad pero no hay resumen, calcularlo
                    summary = dailySummaryService.calculateDailySummary(user, currentDate);
                    progress = summary.getProgressPercentage();
                }
            } else if (currentDate.equals(today)) {
                // Día actual: calcular en tiempo real
                DailySummary summary = dailySummaryService.calculateDailySummary(user, currentDate);
                progress = summary.getProgressPercentage();
            }

            // Construir info del día
            Map<String, Object> dayInfo = new HashMap<>();
            dayInfo.put("date", currentDate);
            dayInfo.put("hasActivity", hasActivity);
            dayInfo.put("taskCount", taskCount);
            dayInfo.put("eventCount", eventCount);
            dayInfo.put("progress", progress);
            dayInfo.put("isToday", currentDate.equals(today));
            dayInfo.put("isPast", currentDate.isBefore(today));

            daysMap.put(currentDate, dayInfo);
        }

        // Respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("year", year);
        response.put("month", month);
        response.put("days", daysMap.values());

        return response;
    }

    /**
     * Obtener detalle completo de un día específico
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDayDetails(LocalDate date) {

        User user = securityUtil.getCurrentUser();

        log.info("📆 Obteniendo detalle de {} para {}", date, user.getEmail());

        // 1️⃣ Tareas del día
        List<Task> tasks = taskRepository.findByUserIdAndFechaLimite(user.getId(), date);
        List<TaskResponse> taskResponses = tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        // 2️⃣ Eventos del día
        List<Event> events = eventRepository.findByUserIdAndFecha(user.getId(), date);
        List<EventResponse> eventResponses = events.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        // 3️⃣ Hábitos del día
        List<Habit> activeHabits = habitRepository.findByUserIdAndActivoTrue(user.getId());
        List<HabitWithProgressResponse> habitResponses = activeHabits.stream()
                .map(habit -> {
                    HabitLog log = habitLogRepository
                            .findByHabitIdAndFecha(habit.getId(), date)
                            .orElse(null);
                    return mapToHabitWithProgress(habit, log);
                })
                .collect(Collectors.toList());

        // 4️⃣ Resumen del día
        DailySummary summary = dailySummaryService.getOrCalculateDailySummary(user, date);

        // Respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("fecha", date);
        response.put("tareas", taskResponses);
        response.put("eventos", eventResponses);
        response.put("habitos", habitResponses);
        response.put("progreso", summary.getProgressPercentage());
        response.put("totalTareas", summary.getTotalTasks());
        response.put("tareasCompletadas", summary.getCompletedTasks());
        response.put("totalHabitos", summary.getTotalHabits());
        response.put("habitosCompletados", summary.getCompletedHabits());

        return response;
    }

    // ============================================
    // MAPPERS (reutilizar de DashboardService)
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