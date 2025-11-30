// ============================================
// TaskService.java - Gestión de Tareas
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateTaskRequest;
import com.tecsup.productivity.dto.request.UpdateTaskRequest;
import com.tecsup.productivity.dto.response.TaskResponse;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.exception.ResourceNotFoundException;
import com.tecsup.productivity.exception.UnauthorizedException;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(Boolean completed, Task.TaskPriority prioridad) {
        User user = securityUtil.getCurrentUser();
        List<Task> tasks;

        if (completed != null && prioridad != null) {
            tasks = taskRepository.findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(
                            user.getId(), completed)
                    .stream()
                    .filter(t -> t.getPrioridad().equals(prioridad))
                    .collect(Collectors.toList());
        } else if (completed != null) {
            tasks = taskRepository.findByUserIdAndCompletedOrderByPrioridadAscCreatedAtDesc(
                    user.getId(), completed);
        } else if (prioridad != null) {
            tasks = taskRepository.findByUserIdAndPrioridadOrderByCreatedAtDesc(
                    user.getId(), prioridad);
        } else {
            tasks = taskRepository.findByUserIdOrderByPrioridadAscCreatedAtDesc(user.getId());
        }

        return tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        Task task = findTaskById(id);
        validateOwnership(task);
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        User user = securityUtil.getCurrentUser();

        Task task = Task.builder()
                .user(user)
                .titulo(request.getTitulo().trim())
                .descripcion(request.getDescripcion())
                .prioridad(request.getPrioridad())
                .fechaLimite(request.getFechaLimite())
                .completed(false)
                .source("user") // ✅ Siempre "user" cuando se crea manualmente
                .sincronizadoTecsup(false)
                .build();

        task = taskRepository.save(task);
        log.info("[TASK] Tarea creada manualmente: {} por usuario {}",
                task.getId(), user.getId());

        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = findTaskById(id);
        validateOwnership(task);

        // ✅ Validar que no es tarea sincronizada de TECSUP
        if ("tecsup".equals(task.getSource())) {
            // Permitir cambiar SOLO completed y prioridad (campos locales)
            if (request.getCompleted() != null) {
                task.setCompleted(request.getCompleted());
            }
            if (request.getPrioridad() != null) {
                task.setPrioridad(request.getPrioridad());
            }
            task = taskRepository.save(task);
            return mapToTaskResponse(task);
        }

        // Para tareas "user", permitir editar todo
        if (request.getTitulo() != null && !request.getTitulo().isBlank()) {
            task.setTitulo(request.getTitulo().trim());
        }

        if (request.getDescripcion() != null) {
            task.setDescripcion(request.getDescripcion().trim());
        }

        if (request.getPrioridad() != null) {
            task.setPrioridad(request.getPrioridad());
        }

        if (request.getFechaLimite() != null) {
            task.setFechaLimite(request.getFechaLimite());
        }

        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
        }

        task = taskRepository.save(task);
        return mapToTaskResponse(task);
    }

    @Transactional
    public TaskResponse toggleComplete(Long id) {
        Task task = findTaskById(id);
        validateOwnership(task);

        // ✅ Permitir toggle incluso en tareas TECSUP
        task.setCompleted(!task.getCompleted());
        task = taskRepository.save(task);

        log.info("[TASK] Tarea {} marcada como {}",
                id, task.getCompleted() ? "completada" : "pendiente");

        return mapToTaskResponse(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = findTaskById(id);
        validateOwnership(task);

        // ✅ Validar que no es tarea sincronizada de TECSUP
        if ("tecsup".equals(task.getSource())) {
            throw new BadRequestException(
                    "No puedes eliminar tareas sincronizadas desde TECSUP. " +
                            "Desactiva la sincronización o elimina en Canvas."
            );
        }

        taskRepository.delete(task);
        log.info("[TASK] Tarea eliminada: {} por usuario {}",
                id, securityUtil.getCurrentUserId());
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada"));
    }

    private void validateOwnership(Task task) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (!task.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("No tienes permiso para acceder a esta tarea");
        }
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .titulo(task.getTitulo())
                .descripcion(task.getDescripcion())
                .prioridad(task.getPrioridad())
                .fechaLimite(task.getFechaLimite())
                .completed(task.getCompleted())
                .source(task.getSource()) // ✅ NUEVO
                .tecsupExternalId(task.getTecsupExternalId()) // ✅ NUEVO
                .sincronizadoTecsup(task.getSincronizadoTecsup())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt()) // ✅ NUEVO
                .build();
    }
}