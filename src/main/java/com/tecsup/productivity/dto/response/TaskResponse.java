// ============================================
// TaskResponse.java
// ============================================
package com.tecsup.productivity.dto.response;

import com.tecsup.productivity.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private Long userId;
    private String titulo;
    private String descripcion;
    private Task.TaskPriority prioridad;
    private LocalDate fechaLimite;
    private Boolean completed;
    private LocalDateTime createdAt;
}