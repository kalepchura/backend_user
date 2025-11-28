package com.tecsup.productivity.dto.response;

import com.tecsup.productivity.entity.Habit;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitWithProgressResponse {

    private Long id;
    private String nombre;
    private Habit.HabitType tipo;   // ← CAMBIO IMPORTANTE
    private Boolean esComida;
    private Integer metaDiaria;
    private Boolean activo;

    private Boolean completado;
    private Integer valorActual;
    private Integer progreso;
}
