package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByUserIdOrderByFechaAscHoraAsc(Long userId);

    List<Event> findByUserIdAndFechaOrderByHoraAsc(Long userId, LocalDate fecha);

    List<Event> findByUserIdAndCategoriaOrderByFechaAscHoraAsc(
            Long userId,
            Event.EventCategory categoria
    );

    List<Event> findByUserIdAndFechaBetweenOrderByFechaAscHoraAsc(
            Long userId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.fecha = :fecha ORDER BY e.hora ASC")
    List<Event> findEventsByUserAndDate(
            @Param("userId") Long userId,
            @Param("fecha") LocalDate fecha
    );

    // Para sincronización TECSUP
    void deleteByUserIdAndSincronizadoTecsup(Long userId, Boolean sincronizado);
}