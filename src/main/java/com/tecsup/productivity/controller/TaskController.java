// ============================================
// TaskController.java - EP-04
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.CreateTaskRequest;
import com.tecsup.productivity.dto.request.UpdateTaskRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.TaskResponse;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Task.TaskPriority prioridad
    ) {
        List<TaskResponse> response = taskService.getTasks(completed, prioridad);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        TaskResponse response = taskService.getTask(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tarea creada exitosamente", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Tarea actualizada exitosamente", response)
        );
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<TaskResponse>> toggleComplete(@PathVariable Long id) {
        TaskResponse response = taskService.toggleComplete(id);
        return ResponseEntity.ok(
                ApiResponse.success("Estado actualizado", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(
                ApiResponse.success("Tarea eliminada exitosamente", null)
        );
    }
}