package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Habit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHabitRequest {
    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotNull(message = "El tipo es requerido")
    private Habit.HabitType tipo;

    @NotNull(message = "Debe especificar si es comida")
    private Boolean esComida;

    private Integer metaDiaria; // Obligatorio si esComida = false
}