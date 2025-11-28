// ============================================
// DashboardResponse.java - HU-4, CA-07
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
public class DashboardResponse {

    private LocalDate fecha;

    private List<TaskResponse> tareas;
    private List<EventResponse> eventos;
    private List<HabitWithProgressResponse> habitos;

    private List<TaskResponse> tareasVencidas;

    private Integer progresoDelDia;

    private Integer totalTareas;
    private Integer tareasCompletadas;

    private Integer totalHabitos;
    private Integer habitosCompletados;
}