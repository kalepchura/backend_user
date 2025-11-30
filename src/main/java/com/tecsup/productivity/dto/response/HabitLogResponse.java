package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLogResponse {
    private Long id;
    private Long habitId;
    private LocalDate fecha;
    private Boolean completado;
    private Integer valor;
    private LocalDateTime createdAt;
}