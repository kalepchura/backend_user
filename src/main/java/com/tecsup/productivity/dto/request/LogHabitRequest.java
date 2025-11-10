// ============================================
// LogHabitRequest.java - HU-9, CA-12
// ============================================
package com.tecsup.productivity.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogHabitRequest {

    @NotNull(message = "El ID del hábito es requerido")
    private Long habitId;

    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    @NotNull(message = "El estado es requerido")
    private Boolean completado;

    private Integer valor;
}
