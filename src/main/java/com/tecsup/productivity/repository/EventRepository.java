package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // ✅ Métodos existentes
    List<Event> findByUserIdOrderByFechaAscHoraAsc(Long userId);

    // ============================================
    // ✅ MÉTODOS DE SINCRONIZACIÓN
    // ============================================

    /**
     * Eliminar eventos sincronizados (método antiguo - mantener compatibilidad)
     */
    @Modifying
    @Query("DELETE FROM Event e WHERE e.user.id = :userId AND e.sincronizadoTecsup = :sincronizado")
    void deleteByUserIdAndSincronizadoTecsup(
            @Param("userId") Long userId,
            @Param("sincronizado") Boolean sincronizado
    );

    /**
     * ✅ NUEVO - Eliminar por source (user o tecsup)
     */


    /**
     * ✅ NUEVO - Obtener eventos por source
     */
    List<Event> findByUserIdAndSource(Long userId, String source);

    /**
     * ✅ NUEVO - Obtener eventos por categoría
     */
    List<Event> findByUserIdAndCategoriaOrderByFechaAscHoraAsc(
            Long userId,
            Event.EventCategory categoria
    );

    /**
     * ✅ NUEVO - Obtener eventos de un rango de fechas
     */
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.fecha BETWEEN :startDate AND :endDate " +
            "ORDER BY e.fecha ASC, e.hora ASC")
    List<Event> findByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Event> findByUserId(Long userId);

    void deleteByUserIdAndSource(Long userId, String source);

    // ============================================
    // CONSULTAS POR FECHA
    // ============================================

    /**
     * Eventos de un día específico
     */
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.fecha = :date " +
            "ORDER BY e.hora ASC")
    List<Event> findByUserIdAndFecha(
            @Param("userId") Long userId,
            @Param("date") LocalDate date
    );

    /**
     * Eventos en un rango de fechas
     */
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.fecha BETWEEN :startDate AND :endDate " +
            "ORDER BY e.fecha ASC, e.hora ASC")
    List<Event> findByUserIdAndFechaBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // CONSULTAS POR CATEGORÍA
    // ============================================

    /**
     * Eventos por categoría en un rango de fechas
     */
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.categoria = :categoria " +
            "AND e.fecha BETWEEN :startDate AND :endDate " +
            "ORDER BY e.fecha ASC, e.hora ASC")
    List<Event> findByUserIdAndCategoriaAndFechaBetween(
            @Param("userId") Long userId,
            @Param("categoria") Event.EventCategory categoria,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ============================================
    // CONTADORES
    // ============================================

    /**
     * Contar eventos de un día
     */
    long countByUserIdAndFecha(Long userId, LocalDate date);

    /**
     * Verificar si hay eventos en un día
     */
    boolean existsByUserIdAndFecha(Long userId, LocalDate date);

    /**
     * Contar eventos por categoría en un día
     */
    long countByUserIdAndFechaAndCategoria(
            Long userId,
            LocalDate date,
            Event.EventCategory categoria
    );

    /**
     * Encontrar eventos de un usuario en una fecha ordenados por hora
     */
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.fecha = :fecha " +
            "ORDER BY e.hora ASC")
    List<Event> findByUserIdAndFechaOrderByHoraAsc(
            @Param("userId") Long userId,
            @Param("fecha") LocalDate fecha
    );
}