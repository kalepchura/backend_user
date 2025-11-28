package com.tecsup.productivity.dto.response;

import com.tecsup.productivity.entity.Habit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitResponse {

    private Long id;
    private Long userId;
    private String nombre;
    private Habit.HabitType tipo;
    private Integer metaDiaria;


    // ✅ NUEVOS campos
    private Boolean esComida;
    private Boolean activo;

    private LocalDateTime createdAt;
}