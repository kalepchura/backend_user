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

    Optional<HabitLog> findByHabitIdAndFecha(Long habitId, LocalDate fecha);

    List<HabitLog> findByHabitIdAndFechaBetweenOrderByFechaAsc(
            Long habitId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    @Query("SELECT hl FROM HabitLog hl WHERE hl.habit.id = :habitId " +
            "AND hl.fecha >= :fechaInicio " +
            "ORDER BY hl.fecha DESC")
    List<HabitLog> findRecentLogsByHabit(
            @Param("habitId") Long habitId,
            @Param("fechaInicio") LocalDate fechaInicio
    );

    @Query("SELECT hl FROM HabitLog hl " +
            "JOIN hl.habit h " +
            "WHERE h.user.id = :userId " +
            "AND hl.fecha = :fecha")
    List<HabitLog> findLogsByUserAndDate(
            @Param("userId") Long userId,
            @Param("fecha") LocalDate fecha
    );
}