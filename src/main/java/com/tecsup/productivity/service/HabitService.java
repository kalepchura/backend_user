// ============================================
// HabitService.java - VERSIÓN CORREGIDA
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
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.exception.ResourceNotFoundException;
import com.tecsup.productivity.exception.UnauthorizedException;
import com.tecsup.productivity.repository.HabitLogRepository;
import com.tecsup.productivity.repository.HabitRepository;
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
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<HabitResponse> getHabits() {
        User user = securityUtil.getCurrentUser();

        // ✅ Solo devolver hábitos activos por defecto
        List<Habit> habits = habitRepository.findByUserIdAndActivoTrueOrderByCreatedAtDesc(user.getId());

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

        // ✅ Determinar si es comida
        boolean esComida = request.getTipo() == Habit.HabitType.DESAYUNO ||
                request.getTipo() == Habit.HabitType.ALMUERZO ||
                request.getTipo() == Habit.HabitType.CENA;

        // ✅ Para comidas, metaDiaria puede ser NULL
        Integer metaDiaria = esComida ? null : request.getMetaDiaria();

        Habit habit = Habit.builder()
                .user(user)
                .nombre(request.getNombre().trim())
                .tipo(request.getTipo())
                .metaDiaria(metaDiaria)
                .esComida(esComida)
                .activo(true)
                .build();

        habit = habitRepository.save(habit);
        log.info("[HABIT] Hábito creado: {} (tipo: {}) por usuario {}",
                habit.getId(), habit.getTipo(), user.getId());

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

            // ✅ Actualizar esComida si cambia el tipo
            boolean esComida = request.getTipo() == Habit.HabitType.DESAYUNO ||
                    request.getTipo() == Habit.HabitType.ALMUERZO ||
                    request.getTipo() == Habit.HabitType.CENA;
            habit.setEsComida(esComida);
        }

        if (request.getMetaDiaria() != null) {
            habit.setMetaDiaria(request.getMetaDiaria());
        }

        // ✅ Permitir activar/desactivar
        if (request.getActivo() != null) {
            habit.setActivo(request.getActivo());
        }

        habit = habitRepository.save(habit);
        return mapToHabitResponse(habit);
    }

    @Transactional
    public void deleteHabit(Long id) {
        Habit habit = findHabitById(id);
        validateOwnership(habit);

        // ✅ En lugar de eliminar, desactivar
        habit.setActivo(false);
        habitRepository.save(habit);

        log.info("[HABIT] Hábito desactivado: {} por usuario {}",
                id, securityUtil.getCurrentUserId());
    }

    @Transactional
    public HabitLogResponse logHabit(LogHabitRequest request) {
        Habit habit = findHabitById(request.getHabitId());
        validateOwnership(habit);

        // ✅ Solo permitir registrar el día actual
        if (!request.getFecha().equals(LocalDate.now())) {
            throw new BadRequestException("Solo puedes registrar hábitos del día actual");
        }

        // Buscar o crear log del día
        HabitLog habitLog = habitLogRepository
                .findByHabitIdAndFecha(request.getHabitId(), request.getFecha())
                .orElse(HabitLog.builder()
                        .habit(habit)
                        .fecha(request.getFecha())
                        .build());

        habitLog.setCompletado(request.getCompletado());
        habitLog.setValor(request.getValor());

        habitLog = habitLogRepository.save(habitLog);

        // ✅ CORREGIDO: log.info en lugar de log
        log.info("[HABIT] Log registrado: hábito {} en fecha {} por usuario {}",
                habit.getId(), request.getFecha(), securityUtil.getCurrentUserId());

        return mapToHabitLogResponse(habitLog);
    }

    @Transactional(readOnly = true)
    public HabitProgressResponse getHabitProgress(Long id, Integer days) {
        Habit habit = findHabitById(id);
        validateOwnership(habit);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<HabitLog> logs = habitLogRepository.findByHabitAndDateRange(
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
                .esComida(habit.getEsComida())
                .activo(habit.getActivo())
                .createdAt(habit.getCreatedAt())
                .build();
    }

    private HabitLogResponse mapToHabitLogResponse(HabitLog habitLog) {
        return HabitLogResponse.builder()
                .id(habitLog.getId())
                .habitId(habitLog.getHabit().getId())
                .fecha(habitLog.getFecha())
                .completado(habitLog.getCompletado())
                .valor(habitLog.getValor())
                .createdAt(habitLog.getCreatedAt())
                .build();
    }
}