package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateHabitRequest;
import com.tecsup.productivity.dto.request.LogHabitRequest;
import com.tecsup.productivity.dto.request.UpdateHabitRequest;
import com.tecsup.productivity.dto.response.HabitProgressResponse;
import com.tecsup.productivity.dto.response.HabitResponse;
import com.tecsup.productivity.dto.response.HabitWithProgressResponse;
import com.tecsup.productivity.entity.Habit;
import com.tecsup.productivity.entity.HabitLog;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.exception.ResourceNotFoundException;
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

    // ============================================
    // PANTALLA BIENESTAR - OBTENER HÁBITOS DEL DÍA
    // ============================================

    /**
     * Obtener hábitos de hoy con su progreso
     * Para la pantalla de BIENESTAR
     */
    @Transactional(readOnly = true)
    public List<HabitWithProgressResponse> getTodayHabits() {
        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();

        log.info("📋 Obteniendo hábitos de hoy para: {}", user.getEmail());

        List<Habit> habits = habitRepository.findByUserIdAndActivoTrue(user.getId());

        return habits.stream()
                .map(habit -> {
                    HabitLog log = habitLogRepository
                            .findByHabitIdAndFecha(habit.getId(), today)
                            .orElse(null);
                    return mapToHabitWithProgress(habit, log);
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtener resumen de ayer
     * Para comparar en BIENESTAR
     */
    @Transactional(readOnly = true)
    public HabitProgressResponse getYesterdaySummary() {
        User user = securityUtil.getCurrentUser();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        log.info("📊 Obteniendo resumen de ayer para: {}", user.getEmail());

        List<Habit> habits = habitRepository.findByUserIdAndActivoTrue(user.getId());
        List<HabitLog> logs = habitLogRepository.findByUserAndDate(user.getId(), yesterday);

        int total = habits.size();
        int completed = (int) logs.stream().filter(HabitLog::getCompletado).count();
        int progress = (total > 0) ? Math.round((completed * 100.0f) / total) : 0;

        return HabitProgressResponse.builder()
                .fecha(yesterday)
                .totalHabitos(total)
                .habitosCompletados(completed)
                .progreso(progress)
                .build();
    }

    /**
     * Obtener histórico de hábitos (últimos N días)
     * Para gráficas en BIENESTAR
     */
    @Transactional(readOnly = true)
    public List<HabitProgressResponse> getHabitHistory(int days) {
        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        log.info("📈 Obteniendo histórico de {} días para: {}", days, user.getEmail());

        List<Habit> habits = habitRepository.findByUserIdAndActivoTrue(user.getId());
        int totalHabits = habits.size();

        return startDate.datesUntil(today.plusDays(1))
                .map(date -> {
                    List<HabitLog> logs = habitLogRepository.findByUserAndDate(user.getId(), date);
                    int completed = (int) logs.stream().filter(HabitLog::getCompletado).count();
                    int progress = (totalHabits > 0) ? Math.round((completed * 100.0f) / totalHabits) : 0;

                    return HabitProgressResponse.builder()
                            .fecha(date)
                            .totalHabitos(totalHabits)
                            .habitosCompletados(completed)
                            .progreso(progress)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ============================================
    // CRUD HÁBITOS
    // ============================================

    /**
     * Crear hábito personalizado
     */
    @Transactional
    public HabitResponse createHabit(CreateHabitRequest request) {
        User user = securityUtil.getCurrentUser();

        log.info("➕ Creando hábito: {} para {}", request.getNombre(), user.getEmail());

        // Validar que no exista un hábito del mismo tipo activo
        if (habitRepository.existsByUserIdAndTipoAndActivoTrue(user.getId(), request.getTipo())) {
            throw new BadRequestException("Ya tienes un hábito activo de tipo " + request.getTipo());
        }

        Habit habit = Habit.builder()
                .user(user)
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .esComida(request.getEsComida())
                .metaDiaria(request.getMetaDiaria())
                .activo(true)
                .build();

        habit = habitRepository.save(habit);
        log.info("✅ Hábito creado: {}", habit.getNombre());

        return mapToHabitResponse(habit);
    }

    /**
     * Actualizar hábito
     */
    @Transactional
    public HabitResponse updateHabit(Long habitId, UpdateHabitRequest request) {
        User user = securityUtil.getCurrentUser();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Hábito no encontrado"));

        if (!habit.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permiso para editar este hábito");
        }

        log.info("✏️ Actualizando hábito: {} ({})", habit.getNombre(), habitId);

        if (request.getNombre() != null) {
            habit.setNombre(request.getNombre());
        }
        if (request.getMetaDiaria() != null) {
            habit.setMetaDiaria(request.getMetaDiaria());
        }
        if (request.getActivo() != null) {
            habit.setActivo(request.getActivo());
        }

        habit = habitRepository.save(habit);
        log.info("✅ Hábito actualizado: {}", habit.getNombre());

        return mapToHabitResponse(habit);
    }

    /**
     * Eliminar hábito
     */
    @Transactional
    public void deleteHabit(Long habitId) {
        User user = securityUtil.getCurrentUser();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Hábito no encontrado"));

        if (!habit.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permiso para eliminar este hábito");
        }

        log.info("🗑️ Eliminando hábito: {} ({})", habit.getNombre(), habitId);

        // Eliminar logs asociados
        habitLogRepository.deleteByHabitId(habitId);
        habitRepository.delete(habit);

        log.info("✅ Hábito eliminado");
    }

    /**
     * Desactivar hábito (soft delete)
     */
    @Transactional
    public HabitResponse deactivateHabit(Long habitId) {
        User user = securityUtil.getCurrentUser();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Hábito no encontrado"));

        if (!habit.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permiso para desactivar este hábito");
        }

        log.info("⏸️ Desactivando hábito: {}", habit.getNombre());

        habit.setActivo(false);
        habit = habitRepository.save(habit);

        return mapToHabitResponse(habit);
    }

    // ============================================
    // REGISTRAR PROGRESO DE HÁBITOS
    // ============================================

    /**
     * Registrar progreso de un hábito (comida o numérico)
     */
    @Transactional
    public HabitWithProgressResponse logHabitProgress(Long habitId, LogHabitRequest request) {
        User user = securityUtil.getCurrentUser();
        LocalDate fecha = request.getFecha() != null ? request.getFecha() : LocalDate.now();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Hábito no encontrado"));

        if (!habit.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permiso para registrar este hábito");
        }

        log.info("📝 Registrando progreso: {} - {} (valor: {})",
                habit.getNombre(), fecha, request.getValor());

        // Buscar o crear log del día
        HabitLog log = habitLogRepository.findByHabitIdAndFecha(habitId, fecha)
                .orElse(HabitLog.builder()
                        .habit(habit)
                        .fecha(fecha)
                        .completado(false)
                        .valor(0)
                        .build());

        // Actualizar valor
        if (request.getValor() != null) {
            log.setValor(request.getValor());
        }

        // Determinar si está completado
        if (habit.getEsComida()) {
            // Comida: con cualquier registro se marca como completado
            log.setCompletado(request.getValor() != null && request.getValor() > 0);
        } else {
            // Numérico: se completa si alcanza la meta
            if (habit.getMetaDiaria() != null && log.getValor() != null) {
                log.setCompletado(log.getValor() >= habit.getMetaDiaria());
            }
        }

        log = habitLogRepository.save(log);
        log.info("✅ Progreso registrado: {} - Completado: {}", habit.getNombre(), log.getCompletado());

        return mapToHabitWithProgress(habit, log);
    }

    /**
     * Marcar hábito como completado directamente (toggle)
     */
    @Transactional
    public HabitWithProgressResponse toggleHabitCompletion(Long habitId) {
        User user = securityUtil.getCurrentUser();
        LocalDate today = LocalDate.now();

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Hábito no encontrado"));

        if (!habit.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("No tienes permiso para modificar este hábito");
        }

        // Buscar o crear log
        HabitLog log = habitLogRepository.findByHabitIdAndFecha(habitId, today)
                .orElse(HabitLog.builder()
                        .habit(habit)
                        .fecha(today)
                        .completado(false)
                        .valor(0)
                        .build());

        // Toggle completado
        log.setCompletado(!log.getCompletado());

        // Si es numérico y se marca como completado, establecer valor = meta
        if (log.getCompletado() && !habit.getEsComida() && habit.getMetaDiaria() != null) {
            log.setValor(habit.getMetaDiaria());
        }

        log = habitLogRepository.save(log);
        log.info("✅ Hábito {} - Completado: {}", habit.getNombre(), log.getCompletado());

        return mapToHabitWithProgress(habit, log);
    }

    // ============================================
    // MAPPERS
    // ============================================

    private HabitResponse mapToHabitResponse(Habit habit) {
        return HabitResponse.builder()
                .id(habit.getId())
                .nombre(habit.getNombre())
                .tipo(habit.getTipo())
                .esComida(habit.getEsComida())
                .metaDiaria(habit.getMetaDiaria())
                .activo(habit.getActivo())
                .createdAt(habit.getCreatedAt())
                .build();
    }

    private HabitWithProgressResponse mapToHabitWithProgress(Habit habit, HabitLog log) {
        Integer valorActual = (log != null && log.getValor() != null) ? log.getValor() : 0;
        Boolean completado = (log != null) && log.getCompletado();

        Integer progreso = calculateProgress(habit, valorActual, completado);

        return HabitWithProgressResponse.builder()
                .id(habit.getId())
                .nombre(habit.getNombre())
                .tipo(habit.getTipo())
                .esComida(habit.getEsComida())
                .metaDiaria(habit.getMetaDiaria())
                .activo(habit.getActivo())
                .completado(completado)
                .valorActual(valorActual)
                .progreso(progreso)
                .build();
    }

    private Integer calculateProgress(Habit habit, Integer valorActual, Boolean completado) {
        if (habit.getEsComida()) {
            return completado ? 100 : 0;
        }

        if (habit.getMetaDiaria() == null || habit.getMetaDiaria() == 0) {
            return 0;
        }

        int progress = Math.round((valorActual * 100.0f) / habit.getMetaDiaria());
        return Math.min(progress, 100);
    }
}