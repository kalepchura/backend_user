package com.tecsup.productivity.dto.response;

import com.tecsup.productivity.entity.Habit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitResponse {
    private Long id;
    private String nombre;
    private Habit.HabitType tipo;
    private Boolean esComida;
    private Integer metaDiaria;
    private Boolean activo;
    private LocalDateTime createdAt;
}