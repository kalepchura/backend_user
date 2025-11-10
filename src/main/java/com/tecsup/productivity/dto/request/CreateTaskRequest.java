// ============================================
// CreateTaskRequest.java - HU-8, CA-11
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Task;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "El título es requerido")
    @Size(min = 3, message = "Mínimo 3 caracteres")
    private String titulo;

    private String descripcion;

    @NotNull(message = "La prioridad es requerida")
    private Task.TaskPriority prioridad;

    private LocalDate fechaLimite;
}
