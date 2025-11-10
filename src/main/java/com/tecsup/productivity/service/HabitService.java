// ============================================
// HabitService.java - EP-04 (HU-9, CA-12)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateHabitRequest;
import com.tecsup.productivity.dto.request.LogHabitRequest;
import com.tecsup.productivity.dto.request.UpdateHabitRequest;
import com.tecsup.productivity.dto.response.HabitLogResponse;
import com.tecsup.productivity.dto.response.HabitProgressResponse;
import com.tecsup.productivity.dto.response.HabitResponse;
import com.tecsup.productivity.entity.Habit;
import com.tecsup.productivity.entity.HabitLog;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.ResourceNotFoundException;
import com.tecsup.productivity.exception.UnauthorizedException;
import com.tecsup.productivity.repository.HabitLogRepository;
import com.tecsup.productivity.repository.HabitRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<HabitResponse> getHabits() {
        User user = securityUtil.getCurrentUser();
        List<Habit> habits = habitRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return habits.stream()
                .map(this::mapToHabitResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HabitResponse getHabit(Long id) {
        Habit habit = findHabitById(id);
        validateOwnership(habit);
        return mapToHabitResponse(habit);
    }

    @Transactional
    public HabitResponse createHabit(CreateHabitRequest request) {
        User user = securityUtil.getCurrentUser();

        Habit habit = Habit.builder()
                .user(user)
                .nombre(request.getNombre().trim())
                .tipo(request.getTipo())
                .metaDiaria(request.getMetaDiaria())
                .build();

        habit = habitRepository.save(habit);
        return mapToHabitResponse(habit);
    }

    @Transactional
    public HabitResponse updateHabit(Long id, UpdateHabitRequest request) {
        Habit habit = findHabitById(id);
        validateOwnership(habit);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            habit.setNombre(request.getNombre().trim());
        }

        if (request.getTipo() != null) {
            habit.setTipo(request.getTipo());
        }

        if (request.getMetaDiaria() != null) {
            habit.setMetaDiaria(request.getMetaDiaria());
        }

        habit = habitRepository.save(habit);
        return mapToHabitResponse(habit);
    }

    @Transactional
    public void deleteHabit(Long id) {
        Habit habit = findHabitById(id);
        validateOwnership(habit);
        habitRepository.delete(habit);
    }

    // Registrar progreso diario
    @Transactional
    public HabitLogResponse logHabit(LogHabitRequest request) {
        Habit habit = findHabitById(request.getHabitId());
        validateOwnership(habit);

        // Buscar o crear log del día
        HabitLog log = habitLogRepository
                .findByHabitIdAndFecha(request.getHabitId(), request.getFecha())
                .orElse(HabitLog.builder()
                        .habit(habit)
                        .fecha(request.getFecha())
                        .build());

        log.setCompletado(request.getCompletado());
        log.setValor(request.getValor());

        log = habitLogRepository.save(log);
        return mapToHabitLogResponse(log);
    }

    // Obtener progreso semanal (CA-12)
    @Transactional(readOnly = true)
    public HabitProgressResponse getHabitProgress(Long id, Integer days) {
        Habit habit = findHabitById(id);
        validateOwnership(habit);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<HabitLog> logs = habitLogRepository.findByHabitIdAndFechaBetweenOrderByFechaAsc(
                id, startDate, endDate);

        List<HabitLogResponse> logResponses = logs.stream()
                .map(this::mapToHabitLogResponse)
                .collect(Collectors.toList());

        return HabitProgressResponse.builder()
                .habit(mapToHabitResponse(habit))
                .weeklyLogs(logResponses)
                .build();
    }

    private Habit findHabitById(Long id) {
        return habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hábito no encontrado"));
    }

    private void validateOwnership(Habit habit) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (!habit.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("No tienes permiso para acceder a este hábito");
        }
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
}