package com.tecsup.productivity.service;

import com.tecsup.productivity.entity.DailySummary;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.repository.DailySummaryRepository;
import com.tecsup.productivity.repository.HabitLogRepository;
import com.tecsup.productivity.repository.HabitRepository;
import com.tecsup.productivity.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailySummaryService {

    private final DailySummaryRepository summaryRepository;
    private final TaskRepository taskRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository; // ← AÑADIR ESTA DEPENDENCIA

    /**
     * Obtener o crear resumen del día
     * - Si existe en BD → devuelve el histórico
     * - Si no existe → calcula en tiempo real
     */
    @Transactional(readOnly = true)
    public DailySummary getOrCalculateDailySummary(User user, LocalDate date) {

        Optional<DailySummary> existing = summaryRepository.findByUserIdAndDate(user.getId(), date);

        if (existing.isPresent()) {
            log.debug("📊 Resumen encontrado en BD para {} - {}", user.getEmail(), date);
            return existing.get();
        }

        // Calcular en tiempo real
        log.debug("⚡ Calculando resumen en tiempo real para {} - {}", user.getEmail(), date);
        return calculateDailySummary(user, date);
    }

    /**
     * Calcular resumen del día (sin guardar en BD)
     */


    @Transactional(readOnly = true)
    public DailySummary calculateDailySummary(User user, LocalDate date) {

        // Contar tareas del día
        long totalTasks = taskRepository.countByUserIdAndFechaLimite(user.getId(), date);
        long completedTasks = taskRepository.countByUserIdAndCompletedAndFechaLimite(
                user.getId(), true, date
        );

        // ✅ CORREGIDO: Contar TODOS los hábitos activos
        long totalHabits = habitRepository.countByUserIdAndActivoTrue(user.getId());

        // ✅ Contar hábitos completados (los que tienen log y están completados)
        long completedHabits = habitLogRepository.countCompletedByUserAndDate(user.getId(), date);

        // Construir resumen temporal (no persistido)
        DailySummary summary = DailySummary.builder()
                .user(user)
                .date(date)
                .totalTasks((int) totalTasks)
                .completedTasks((int) completedTasks)
                .totalHabits((int) totalHabits)      // ← Ahora será correcto
                .completedHabits((int) completedHabits)
                .build();

        summary.calculateProgress();
        return summary;
    }


    /**
     * Guardar snapshot del día (llamar a las 23:59 o al día siguiente)
     */
    @Transactional
    public DailySummary saveDailySummary(User user, LocalDate date) {

        // Verificar si ya existe
        if (summaryRepository.existsByUserIdAndDate(user.getId(), date)) {
            log.warn("⚠️ Ya existe resumen para {} - {}", user.getEmail(), date);
            return summaryRepository.findByUserIdAndDate(user.getId(), date).orElseThrow();
        }

        // Calcular y guardar
        DailySummary summary = calculateDailySummary(user, date);
        summary = summaryRepository.save(summary);

        log.info("💾 Resumen guardado: {} - Progreso: {}%", date, summary.getProgressPercentage());
        return summary;
    }

    /**
     * Obtener resúmenes de un mes (para vista de calendario)
     */
    @Transactional(readOnly = true)
    public List<DailySummary> getMonthlySummaries(User user, int year, int month) {

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        return summaryRepository.findByUserIdAndDateRange(
                user.getId(),
                startDate,
                endDate
        );
    }

    /**
     * Obtener últimos N días con actividad
     */
    @Transactional(readOnly = true)
    public List<DailySummary> getRecentSummaries(User user, int days) {
        return summaryRepository.findRecentSummaries(user.getId(), days);
    }

    /**
     * Limpiar resúmenes antiguos (opcional - ejecutar periódicamente)
     */
    @Transactional
    public void cleanOldSummaries(int daysToKeep) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        summaryRepository.deleteOlderThan(cutoffDate);
        log.info("🧹 Resúmenes anteriores a {} eliminados", cutoffDate);
    }
}