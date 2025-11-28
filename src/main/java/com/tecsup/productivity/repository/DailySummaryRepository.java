package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {

    /**
     * Buscar resumen de un día específico
     */
    Optional<DailySummary> findByUserIdAndDate(Long userId, LocalDate date);

    /**
     * Verificar si existe resumen para un día
     */
    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    /**
     * Obtener resúmenes de un rango de fechas (para calendario mensual)
     */
    @Query("SELECT ds FROM DailySummary ds " +
            "WHERE ds.user.id = :userId " +
            "AND ds.date BETWEEN :startDate AND :endDate " +
            "ORDER BY ds.date DESC")
    List<DailySummary> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Obtener últimos N días con resumen
     */
    @Query("SELECT ds FROM DailySummary ds " +
            "WHERE ds.user.id = :userId " +
            "ORDER BY ds.date DESC " +
            "LIMIT :limit")
    List<DailySummary> findRecentSummaries(
            @Param("userId") Long userId,
            @Param("limit") int limit
    );

    /**
     * Eliminar resúmenes antiguos (limpieza periódica opcional)
     */
    @Query("DELETE FROM DailySummary ds " +
            "WHERE ds.date < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);
}