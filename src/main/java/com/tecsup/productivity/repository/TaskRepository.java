package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

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

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId " +
            "AND t.completed = false")
    Long countPendingTasksByUser(@Param("userId") Long userId);
}