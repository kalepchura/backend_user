// ============================================
// HabitController.java - EP-04
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.CreateHabitRequest;
import com.tecsup.productivity.dto.request.LogHabitRequest;
import com.tecsup.productivity.dto.request.UpdateHabitRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.HabitLogResponse;
import com.tecsup.productivity.dto.response.HabitProgressResponse;
import com.tecsup.productivity.dto.response.HabitResponse;
import com.tecsup.productivity.service.HabitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HabitResponse>>> getHabits() {
        List<HabitResponse> response = habitService.getHabits();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HabitResponse>> getHabit(@PathVariable Long id) {
        HabitResponse response = habitService.getHabit(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HabitResponse>> createHabit(
            @Valid @RequestBody CreateHabitRequest request
    ) {
        HabitResponse response = habitService.createHabit(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hábito creado exitosamente", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HabitResponse>> updateHabit(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHabitRequest request
    ) {
        HabitResponse response = habitService.updateHabit(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Hábito actualizado exitosamente", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(@PathVariable Long id) {
        habitService.deleteHabit(id);
        return ResponseEntity.ok(
                ApiResponse.success("Hábito eliminado exitosamente", null)
        );
    }

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<HabitLogResponse>> logHabit(
            @Valid @RequestBody LogHabitRequest request
    ) {
        HabitLogResponse response = habitService.logHabit(request);
        return ResponseEntity.ok(
                ApiResponse.success("Progreso registrado", response)
        );
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<HabitProgressResponse>> getHabitProgress(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") Integer days
    ) {
        HabitProgressResponse response = habitService.getHabitProgress(id, days);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
