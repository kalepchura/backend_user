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

    List<Task> findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(
            Long userId,
            Boolean completed
    );

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
    @Modifying
    @Query("DELETE FROM Task t WHERE t.user.id = :userId AND t.source = :source")
    void deleteByUserIdAndSource(
            @Param("userId") Long userId,
            @Param("source") String source
    );

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
}