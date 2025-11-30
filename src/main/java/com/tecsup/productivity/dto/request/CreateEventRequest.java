package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {
    @NotBlank(message = "El título es requerido")
    private String titulo;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    private LocalTime hora;

    @NotNull(message = "La categoría es requerida")
    private Event.EventCategory categoria;

    private String descripcion;
    private String curso;
}