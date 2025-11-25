// ============================================
// UpdateEventRequest.java - CON CAMPO CURSO
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Event;
import jakarta.validation.constraints.Size;
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

    @Size(min = 3, message = "Mínimo 3 caracteres")
    private String titulo;

    private LocalDate fecha;

    private LocalTime hora;

    private Event.EventCategory categoria;

    private String descripcion;

    // ✅ Campo curso OPCIONAL
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String curso;
}