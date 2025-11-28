package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    /**
     * Obtener logs de un hábito en una fecha específica
     */
    Optional<HabitLog> findByHabitIdAndFecha(Long habitId, LocalDate fecha);

    /**
     * Obtener todos los logs de un usuario en una fecha
     */
    @Query("SELECT hl FROM HabitLog hl " +
            "WHERE hl.habit.user.id = :userId " +
            "AND hl.fecha = :fecha")
    List<HabitLog> findByUserAndDate(
            @Param("userId") Long userId,
            @Param("fecha") LocalDate fecha
    );

    @Query("SELECT COUNT(hl) FROM HabitLog hl WHERE hl.habit.user.id = :userId AND hl.fecha = :fecha")
    long countByUserAndDate(@Param("userId") Long userId, @Param("fecha") LocalDate fecha);


    /**
     * Obtener logs de un hábito en un rango de fechas
     */
    @Query("SELECT hl FROM HabitLog hl " +
            "WHERE hl.habit.id = :habitId " +
            "AND hl.fecha BETWEEN :startDate AND :endDate " +
            "ORDER BY hl.fecha DESC")
    List<HabitLog> findByHabitAndDateRange(
            @Param("habitId") Long habitId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Contar hábitos completados de un usuario en una fecha
     */
    @Query("SELECT COUNT(hl) FROM HabitLog hl " +
            "WHERE hl.habit.user.id = :userId " +
            "AND hl.fecha = :fecha " +
            "AND hl.completado = true")
    long countCompletedByUserAndDate(
            @Param("userId") Long userId,
            @Param("fecha") LocalDate fecha
    );

    /**
     * Eliminar logs de un hábito
     */
    void deleteByHabitId(Long habitId);
}