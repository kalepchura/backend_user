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


    // ============================================
    // CONSULTAS BÁSICAS
    // ============================================

    /**
     * Todos los hábitos de un usuario
     */
    List<Habit> findByUserId(Long userId);

    /**
     * Solo hábitos activos de un usuario
     */
    List<Habit> findByUserIdAndActivoTrue(Long userId);

    /**
     * Hábitos por tipo
     */
    @Query("SELECT h FROM Habit h WHERE h.user.id = :userId " +
            "AND h.tipo = :tipo " +
            "AND h.activo = true")
    List<Habit> findByUserIdAndTipo(
            @Param("userId") Long userId,
            @Param("tipo") Habit.HabitType tipo
    );

    // ============================================
    // CONSULTAS POR CATEGORÍA
    // ============================================

    /**
     * Obtener solo hábitos de comida activos
     */
    @Query("SELECT h FROM Habit h WHERE h.user.id = :userId " +
            "AND h.esComida = true " +
            "AND h.activo = true " +
            "ORDER BY h.tipo")
    List<Habit> findFoodHabitsByUserId(@Param("userId") Long userId);

    /**
     * Obtener solo hábitos regulares (no comida)
     */
    @Query("SELECT h FROM Habit h WHERE h.user.id = :userId " +
            "AND h.esComida = false " +
            "AND h.activo = true " +
            "ORDER BY h.tipo")
    List<Habit> findRegularHabitsByUserId(@Param("userId") Long userId);

    // ============================================
    // CONTADORES
    // ============================================

    /**
     * Contar hábitos activos de un usuario
     */
    long countByUserIdAndActivoTrue(Long userId);

    /**
     * Contar hábitos de comida activos
     */
    @Query("SELECT COUNT(h) FROM Habit h WHERE h.user.id = :userId " +
            "AND h.esComida = true " +
            "AND h.activo = true")
    long countFoodHabitsByUserId(@Param("userId") Long userId);

    /**
     * Verificar si un usuario ya tiene un hábito de cierto tipo
     */
    boolean existsByUserIdAndTipoAndActivoTrue(
            Long userId,
            Habit.HabitType tipo
    );
}