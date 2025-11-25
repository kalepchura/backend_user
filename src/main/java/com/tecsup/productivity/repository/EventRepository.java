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

    List<Event> findByUserIdAndFechaOrderByHoraAsc(Long userId, LocalDate fecha);

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
    @Modifying
    @Query("DELETE FROM Event e WHERE e.user.id = :userId AND e.source = :source")
    void deleteByUserIdAndSource(
            @Param("userId") Long userId,
            @Param("source") String source
    );

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
}