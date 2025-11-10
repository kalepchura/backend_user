// ============================================
// UpdateTaskRequest.java - HU-8, CA-11
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTaskRequest {
    private String titulo;
    private String descripcion;
    private Task.TaskPriority prioridad;
    private LocalDate fechaLimite;
    private Boolean completed;
}