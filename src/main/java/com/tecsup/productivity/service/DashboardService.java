// ============================================
// DashboardService.java - EP-02 (HU-4, CA-07)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.response.*;
import com.tecsup.productivity.entity.*;
import com.tecsup.productivity.repository.*;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TaskRepository taskRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final EventRepository eventRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        Long userId = securityUtil.getCurrentUserId();
        LocalDate today = LocalDate.now();

        // Tareas pendientes
        List<Task> pendingTasks = taskRepository.findPendingTasksByUser(userId, today);
        List<TaskResponse> taskResponses = pendingTasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());

        // Hábitos con progreso de hoy
        List<Habit> habits = habitRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<HabitWithProgressResponse> habitResponses = habits.stream()
                .map(habit -> {
                    HabitLogResponse progress = habitLogRepository
                            .findByHabitIdAndFecha(habit.getId(), today)
                            .map(this::mapToHabitLogResponse)
                            .orElse(null);

                    return HabitWithProgressResponse.builder()
                            .habit(mapToHabitResponse(habit))
                            .progressToday(progress)
                            .build();
                })
                .collect(Collectors.toList());

        // Eventos de hoy
        List<Event> todayEvents = eventRepository.findByUserIdAndFechaOrderByHoraAsc(userId, today);
        List<EventResponse> eventResponses = todayEvents.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        return DashboardResponse.builder()
                .tareasPendientes(taskResponses)
                .habitosHoy(habitResponses)
                .eventosHoy(eventResponses)
                .build();
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .userId(task.getUser().getId())
                .titulo(task.getTitulo())
                .descripcion(task.getDescripcion())
                .prioridad(task.getPrioridad())
                .fechaLimite(task.getFechaLimite())
                .completed(task.getCompleted())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private HabitResponse mapToHabitResponse(Habit habit) {
        return HabitResponse.builder()
                .id(habit.getId())
                .userId(habit.getUser().getId())
                .nombre(habit.getNombre())
                .tipo(habit.getTipo())
                .metaDiaria(habit.getMetaDiaria())
                .createdAt(habit.getCreatedAt())
                .build();
    }

    private HabitLogResponse mapToHabitLogResponse(HabitLog log) {
        return HabitLogResponse.builder()
                .id(log.getId())
                .habitId(log.getHabit().getId())
                .fecha(log.getFecha())
                .completado(log.getCompletado())
                .valor(log.getValor())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .userId(event.getUser().getId())
                .titulo(event.getTitulo())
                .fecha(event.getFecha())
                .hora(event.getHora())
                .categoria(event.getCategoria())
                .descripcion(event.getDescripcion())
                .curso(event.getCurso())
                .sincronizadoTecsup(event.getSincronizadoTecsup())
                .createdAt(event.getCreatedAt())
                .build();
    }
}