package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    @NotBlank(message = "El título es requerido")
    private String titulo;

    private String descripcion;

    @NotNull(message = "La prioridad es requerida")
    private Task.TaskPriority prioridad;

    private LocalDate fechaLimite;
}