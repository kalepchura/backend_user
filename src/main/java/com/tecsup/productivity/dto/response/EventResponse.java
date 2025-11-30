package com.tecsup.productivity.dto.response;

import com.tecsup.productivity.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String titulo;
    private LocalDate fecha;
    private LocalTime hora;
    private Event.EventCategory categoria;
    private String descripcion;
    private String curso;
    private String source; // "user" o "tecsup"
    private String tecsupExternalId;
    private Boolean sincronizadoTecsup;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}