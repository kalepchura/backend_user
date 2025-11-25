package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {

    // ✅ Ya existente
    List<Habit> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ============================================
    // ✅ NUEVOS MÉTODOS - Para gestión completa
    // ============================================

    /**
     * Obtener solo hábitos activos del usuario
     */
    List<Habit> findByUserIdAndActivoTrueOrderByCreatedAtDesc(Long userId);

    /**
     * Obtener hábitos por tipo
     */
    List<Habit> findByUserIdAndTipo(Long userId, Habit.HabitType tipo);

    /**
     * Obtener solo comidas activas (para pantalla Bienestar)
     */
    @Query("SELECT h FROM Habit h WHERE h.user.id = :userId " +
            "AND h.esComida = true " +
            "AND h.activo = true " +
            "ORDER BY CASE h.tipo " +
            "  WHEN 'DESAYUNO' THEN 1 " +
            "  WHEN 'ALMUERZO' THEN 2 " +
            "  WHEN 'CENA' THEN 3 " +
            "END")
    List<Habit> findActiveFoodHabitsByUser(@Param("userId") Long userId);

    /**
     * Obtener hábitos no-comida activos
     */
    @Query("SELECT h FROM Habit h WHERE h.user.id = :userId " +
            "AND h.esComida = false " +
            "AND h.activo = true " +
            "ORDER BY h.createdAt ASC")
    List<Habit> findActiveNonFoodHabitsByUser(@Param("userId") Long userId);

    /**
     * Contar hábitos activos del usuario
     */
    long countByUserIdAndActivoTrue(Long userId);
}