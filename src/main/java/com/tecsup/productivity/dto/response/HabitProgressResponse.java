package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitProgressResponse {
    private LocalDate fecha;
    private Integer totalHabitos;
    private Integer habitosCompletados;
    private Integer progreso;
}