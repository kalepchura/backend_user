package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.DashboardResponse;
import com.tecsup.productivity.dto.response.TaskResponse;
import com.tecsup.productivity.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/today
     *
     * Obtener resumen completo del día actual:
     * - Tareas del día
     * - Eventos del día
     * - Hábitos del día con progreso
     * - Tareas vencidas
     * - Progreso general
     *
     * Para: Pantalla HOME (dashboard principal)
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<DashboardResponse>> getTodayDashboard() {
        log.info("🏠 [GET] /api/dashboard/today");

        DashboardResponse dashboard = dashboardService.getTodayDashboard();

        return ResponseEntity.ok(
                ApiResponse.success("Dashboard del día obtenido", dashboard)
        );
    }

    /**
     * GET /api/dashboard/upcoming?days=7
     *
     * Obtener tareas próximas (próximos N días)
     * Default: 7 días
     *
     * Para: Sección de "Próximas tareas" en HOME
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getUpcomingTasks(
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("🔜 [GET] /api/dashboard/upcoming?days={}", days);

        if (days < 1 || days > 30) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("El parámetro 'days' debe estar entre 1 y 30")
            );
        }

        List<TaskResponse> tasks = dashboardService.getUpcomingTasks(days);

        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Tareas de los próximos %d días obtenidas", days),
                        tasks
                )
        );
    }

    /**
     * GET /api/dashboard/overdue
     *
     * Obtener tareas vencidas (no completadas)
     *
     * Para: Alerta de tareas vencidas en HOME
     */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getOverdueTasks() {
        log.info("⚠️ [GET] /api/dashboard/overdue");

        List<TaskResponse> tasks = dashboardService.getOverdueTasks();

        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Tareas vencidas obtenidas (%d)", tasks.size()),
                        tasks
                )
        );
    }
}