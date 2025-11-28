package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ✅ Métodos existentes
    List<Task> findByUserIdOrderByPrioridadAscCreatedAtDesc(Long userId);

    List<Task> findByUserIdAndPrioridadOrderByCreatedAtDesc(
            Long userId,
            Task.TaskPriority prioridad
    );

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.completed = false " +
            "AND (t.fechaLimite IS NULL OR t.fechaLimite >= :today) " +
            "ORDER BY " +
            "CASE t.prioridad " +
            "  WHEN 'ALTA' THEN 1 " +
            "  WHEN 'MEDIA' THEN 2 " +
            "  WHEN 'BAJA' THEN 3 " +
            "END, " +
            "t.fechaLimite ASC NULLS LAST")
    List<Task> findPendingTasksByUser(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    // ============================================
    // ✅ MÉTODOS DE SINCRONIZACIÓN (FALTABAN)
    // ============================================

    /**
     * ✅ Eliminar tareas por source (user o tecsup)
     */
    List<Task> findByUserId(Long userId);

    void deleteByUserIdAndSource(Long userId, String source);

    // ============================================
    // CONSULTAS POR FECHA
    // ============================================

    /**
     * Tareas de un día específico
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.fechaLimite = :date " +
            "ORDER BY t.prioridad DESC, t.fechaLimite ASC")
    List<Task> findByUserIdAndFechaLimite(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    /**
     * Tareas en un rango de fechas
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.fechaLimite BETWEEN :startDate AND :endDate " +
            "ORDER BY t.fechaLimite ASC, t.prioridad DESC")
    List<Task> findByUserIdAndFechaLimiteBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // TAREAS VENCIDAS Y PRÓXIMAS
    // ============================================

    /**
     * Tareas vencidas (no completadas y fecha pasada)
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.fechaLimite < :today " +
            "AND t.completed = false " +
            "ORDER BY t.fechaLimite DESC")
    List<Task> findOverdueTasks(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    /**
     * Tareas próximas (siguientes N días)
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.fechaLimite BETWEEN :startDate AND :endDate " +
            "AND t.completed = false " +
            "ORDER BY t.fechaLimite ASC, t.prioridad DESC")
    List<Task> findUpcomingTasks(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // CONTADORES
    // ============================================

    /**
     * Contar tareas de un día
     */
    long countByUserIdAndFechaLimite(Long userId, LocalDate date);

    /**
     * Contar tareas completadas de un día
     */
    long countByUserIdAndCompletedAndFechaLimite(
            Long userId,
            Boolean completed,
            LocalDate date
    );

    /**
     * Verificar si hay tareas en un día
     */
    boolean existsByUserIdAndFechaLimite(Long userId, LocalDate date);

    /**
     * Eliminar tareas sincronizadas (método antiguo - mantener compatibilidad)
     */
    @Modifying
    @Query("DELETE FROM Task t WHERE t.user.id = :userId AND t.sincronizadoTecsup = :sincronizado")
    void deleteByUserIdAndSincronizadoTecsup(
            @Param("userId") Long userId,
            @Param("sincronizado") Boolean sincronizado
    );

    /**
     * ✅ Obtener tareas por source
     */
    List<Task> findByUserIdAndSource(Long userId, String source);

    /**
     * ✅ Obtener tareas del día (para pantalla Bienestar)
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.fechaLimite = :fecha " +
            "ORDER BY " +
            "CASE t.prioridad " +
            "  WHEN 'ALTA' THEN 1 " +
            "  WHEN 'MEDIA' THEN 2 " +
            "  WHEN 'BAJA' THEN 3 " +
            "END")
    List<Task> findByUserAndDate(
            @Param("userId") Long userId,
            @Param("fecha") LocalDate fecha
    );

    /**
     * ✅ Contar tareas pendientes urgentes (para Dashboard HOME)
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId " +
            "AND t.completed = false " +
            "AND t.prioridad = 'ALTA' " +
            "AND (t.fechaLimite IS NULL OR t.fechaLimite <= :deadline)")
    long countUrgentTasks(
            @Param("userId") Long userId,
            @Param("deadline") LocalDate deadline
    );

    long countByUserId(Long userId);

    /**
     * Contar tareas por estado de completado
     */
    long countByUserIdAndCompleted(Long userId, Boolean completed);

    /**
     * Encontrar tareas no completadas ordenadas por prioridad
     */
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
            "AND t.completed = :completed " +
            "ORDER BY t.prioridad ASC, t.createdAt DESC")
    List<Task> findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("completed") Boolean completed
    );
}