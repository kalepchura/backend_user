// ============================================
// DashboardResponse.java - HU-4, CA-07
// ============================================
package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private List<TaskResponse> tareasPendientes;
    private List<HabitWithProgressResponse> habitosHoy;
    private List<EventResponse> eventosHoy;
}