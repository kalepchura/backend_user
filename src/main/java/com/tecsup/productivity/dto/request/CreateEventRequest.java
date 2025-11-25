// ============================================
// CreateEventRequest.java - CON CAMPO CURSO
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Event;
import jakarta.validation.constraints.*;
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
public class CreateEventRequest {

    @NotBlank(message = "El título es requerido")
    @Size(min = 3, message = "Mínimo 3 caracteres")
    private String titulo;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    private LocalTime hora;

    @NotNull(message = "La categoría es requerida")
    private Event.EventCategory categoria;

    private String descripcion;

    // ✅ Campo curso OPCIONAL (para usuarios que quieran especificarlo manualmente)
    @Size(max = 200, message = "Máximo 200 caracteres")
    private String curso;
}