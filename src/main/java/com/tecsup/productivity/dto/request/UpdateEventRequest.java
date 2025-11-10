// ============================================
// UpdateEventRequest.java - HU-6, CA-09
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventRequest {
    private String titulo;
    private LocalDate fecha;
    private LocalTime hora;
    private Event.EventCategory categoria;
    private String descripcion;
}