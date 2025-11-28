package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * GET /api/calendar/month?year=2025&month=11
     *
     * Obtener vista del mes completo con:
     * - Días con actividades
     * - Progreso de cada día
     * - Contadores de tareas/eventos
     *
     * Para: Pantalla CALENDAR (vista mensual)
     */
    @GetMapping("/month")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthView(
            @RequestParam int year,
            @RequestParam int month
    ) {
        log.info("📅 [GET] /api/calendar/month?year={}&month={}", year, month);

        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Mes inválido. Debe estar entre 1 y 12")
            );
        }

        Map<String, Object> monthView = calendarService.getMonthView(year, month);

        return ResponseEntity.ok(
                ApiResponse.success("Vista de calendario obtenida", monthView)
        );
    }

    /**
     * GET /api/calendar/day?date=2025-11-25
     *
     * Obtener detalle completo de un día específico:
     * - Tareas del día
     * - Eventos del día
     * - Hábitos del día con progreso
     * - Resumen de progreso
     *
     * Para: Pantalla CALENDAR (detalle al hacer clic en un día)
     */
    @GetMapping("/day")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDayDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("📆 [GET] /api/calendar/day?date={}", date);

        Map<String, Object> dayDetails = calendarService.getDayDetails(date);

        return ResponseEntity.ok(
                ApiResponse.success("Detalle del día obtenido", dayDetails)
        );
    }

    /**
     * GET /api/calendar/today
     *
     * Atajo para obtener el día actual
     * Equivalente a /api/calendar/day?date=today
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodayDetails() {
        log.info("📆 [GET] /api/calendar/today");

        LocalDate today = LocalDate.now();
        Map<String, Object> dayDetails = calendarService.getDayDetails(today);

        return ResponseEntity.ok(
                ApiResponse.success("Detalle de hoy obtenido", dayDetails)
        );
    }
}