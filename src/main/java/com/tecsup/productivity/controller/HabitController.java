package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.CreateHabitRequest;
import com.tecsup.productivity.dto.request.LogHabitRequest;
import com.tecsup.productivity.dto.request.UpdateHabitRequest;
import com.tecsup.productivity.dto.response.*;
import com.tecsup.productivity.service.HabitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    // ============================================
    // ENDPOINTS PARA PANTALLA BIENESTAR
    // ============================================

    /**
     * GET /api/habits/today
     *
     * Obtener hábitos de hoy con su progreso actual
     *
     * Para: Pantalla BIENESTAR (lista principal)
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<HabitWithProgressResponse>>> getTodayHabits() {
        log.info("🌱 [GET] /api/habits/today");

        List<HabitWithProgressResponse> habits = habitService.getTodayHabits();

        return ResponseEntity.ok(
                ApiResponse.success("Hábitos de hoy obtenidos", habits)
        );
    }

    /**
     * GET /api/habits/yesterday
     *
     * Obtener resumen de ayer (para comparar)
     *
     * Para: Sección "Ayer terminaste con X%" en BIENESTAR
     */
    @GetMapping("/yesterday")
    public ResponseEntity<ApiResponse<HabitProgressResponse>> getYesterdaySummary() {
        log.info("📊 [GET] /api/habits/yesterday");

        HabitProgressResponse summary = habitService.getYesterdaySummary();

        return ResponseEntity.ok(
                ApiResponse.success("Resumen de ayer obtenido", summary)
        );
    }

    /**
     * GET /api/habits/history?days=7
     *
     * Obtener histórico de progreso (últimos N días)
     * Default: 7 días
     *
     * Para: Gráfico de progreso en BIENESTAR
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<HabitProgressResponse>>> getHabitHistory(
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("📈 [GET] /api/habits/history?days={}", days);

        if (days < 1 || days > 30) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("El parámetro 'days' debe estar entre 1 y 30")
            );
        }

        List<HabitProgressResponse> history = habitService.getHabitHistory(days);

        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Histórico de %d días obtenido", days),
                        history
                )
        );
    }

    // ============================================
    // CRUD HÁBITOS
    // ============================================

    /**
     * POST /api/habits
     *
     * Crear hábito personalizado
     *
     * Para: Botón "Agregar hábito" en BIENESTAR
     */
    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @Valid @RequestBody CreateHabitRequest request
    ) {
        log.info("➕ [POST] /api/habits - {}", request.getNombre());

        HabitResponse habit = habitService.createHabit(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hábito creado exitosamente", habit));
    }

    /**
     * PUT /api/habits/{id}
     *
     * Actualizar hábito (nombre, meta, activación)
     *
     * Para: Editar hábito en BIENESTAR
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HabitResponse>> updateHabit(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHabitRequest request
    ) {
        log.info("✏️ [PUT] /api/habits/{}", id);

        HabitResponse habit = habitService.updateHabit(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Hábito actualizado exitosamente", habit)
        );
    }

    /**
     * DELETE /api/habits/{id}
     *
     * Eliminar hábito permanentemente
     *
     * Para: Botón eliminar en BIENESTAR
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(@PathVariable Long id) {
        log.info("🗑️ [DELETE] /api/habits/{}", id);

        habitService.deleteHabit(id);

        return ResponseEntity.ok(
                ApiResponse.success("Hábito eliminado exitosamente", null)
        );
    }

    /**
     * PATCH /api/habits/{id}/deactivate
     *
     * Desactivar hábito (soft delete)
     *
     * Para: Pausar hábito temporalmente
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<HabitResponse>> deactivateHabit(@PathVariable Long id) {
        log.info("⏸️ [PATCH] /api/habits/{}/deactivate", id);

        HabitResponse habit = habitService.deactivateHabit(id);

        return ResponseEntity.ok(
                ApiResponse.success("Hábito desactivado", habit)
        );
    }

    // ============================================
    // REGISTRAR PROGRESO
    // ============================================

    /**
     * POST /api/habits/{id}/log
     *
     * Registrar progreso de un hábito (numérico o comida)
     * Body: { "valor": 5, "fecha": "2025-11-25" }
     *
     * Para: Actualizar valor de agua/ejercicio/sueño en BIENESTAR
     */
    @PostMapping("/{id}/log")
    public ResponseEntity<ApiResponse<HabitWithProgressResponse>> logHabitProgress(
            @PathVariable Long id,
            @Valid @RequestBody LogHabitRequest request
    ) {
        log.info("📝 [POST] /api/habits/{}/log - valor: {}", id, request.getValor());

        HabitWithProgressResponse habit = habitService.logHabitProgress(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Progreso registrado", habit)
        );
    }

    /**
     * PATCH /api/habits/{id}/toggle
     *
     * Marcar/desmarcar hábito como completado (toggle)
     *
     * Para: Checkbox de completado en BIENESTAR
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<HabitWithProgressResponse>> toggleHabitCompletion(
            @PathVariable Long id
    ) {
        log.info("✅ [PATCH] /api/habits/{}/toggle", id);

        HabitWithProgressResponse habit = habitService.toggleHabitCompletion(id);

        return ResponseEntity.ok(
                ApiResponse.success("Estado actualizado", habit)
        );
    }
}