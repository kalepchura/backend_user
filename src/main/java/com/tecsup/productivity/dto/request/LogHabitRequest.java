package com.tecsup.productivity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogHabitRequest {
    private Integer valor;
    private LocalDate fecha; // Opcional, si es null usa hoy
}