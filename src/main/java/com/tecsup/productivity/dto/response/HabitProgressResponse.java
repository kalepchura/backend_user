// ============================================
// HabitProgressResponse.java - HU-9, CA-12
// ============================================
package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitProgressResponse {
    private HabitResponse habit;
    private List<HabitLogResponse> weeklyLogs;
    private LocalDate fecha;
    private int totalHabitos;
    private int habitosCompletados;
    private Integer progreso;


}
