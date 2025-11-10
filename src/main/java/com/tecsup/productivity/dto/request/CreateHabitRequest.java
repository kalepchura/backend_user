// ============================================
// CreateHabitRequest.java - HU-9, CA-12
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Habit;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateHabitRequest {

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, message = "Mínimo 3 caracteres")
    private String nombre;

    @NotNull(message = "El tipo es requerido")
    private Habit.HabitType tipo;

    @NotNull(message = "La meta diaria es requerida")
    @Min(value = 1, message = "La meta debe ser mayor a 0")
    private Integer metaDiaria;
}