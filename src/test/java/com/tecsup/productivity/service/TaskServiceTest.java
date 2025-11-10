package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateTaskRequest;
import com.tecsup.productivity.dto.request.UpdateTaskRequest;
import com.tecsup.productivity.dto.response.TaskResponse;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.ResourceNotFoundException;
import com.tecsup.productivity.exception.UnauthorizedException;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private TaskService taskService;

    private User mockUser;
    private Task mockTask;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@tecsup.edu.pe")
                .name("Test User")
                .build();

        mockTask = Task.builder()
                .id(1L)
                .user(mockUser)
                .titulo("Completar proyecto")
                .descripcion("Backend Spring Boot")
                .prioridad(Task.TaskPriority.ALTA)
                .fechaLimite(LocalDate.now().plusDays(7))
                .completed(false)
                .build();
    }

    @Test
    @DisplayName("HU-8, CA-11: CRUD funcional - Crear tarea")
    void testCreateTask() {
        // Arrange
        CreateTaskRequest request = CreateTaskRequest.builder()
                .titulo("Nueva tarea")
                .descripcion("Descripción")
                .prioridad(Task.TaskPriority.ALTA)
                .fechaLimite(LocalDate.now().plusDays(5))
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(taskRepository.save(any(Task.class))).thenReturn(mockTask);

        // Act
        TaskResponse response = taskService.createTask(request);

        // Assert
        assertNotNull(response);
        assertEquals("Completar proyecto", response.getTitulo());
        assertEquals(Task.TaskPriority.ALTA, response.getPrioridad());
        assertFalse(response.getCompleted());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("CA-11: Orden por prioridad y fecha")
    void testGetTasksOrderedByPriority() {
        // Arrange
        Task taskAlta = Task.builder()
                .id(1L)
                .user(mockUser)
                .titulo("Tarea Alta")
                .prioridad(Task.TaskPriority.ALTA)
                .completed(false)
                .build();

        Task taskBaja = Task.builder()
                .id(2L)
                .user(mockUser)
                .titulo("Tarea Baja")
                .prioridad(Task.TaskPriority.BAJA)
                .completed(false)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(taskRepository.findByUserIdOrderByPrioridadAscCreatedAtDesc(anyLong()))
                .thenReturn(Arrays.asList(taskAlta, taskBaja));

        // Act
        List<TaskResponse> responses = taskService.getTasks(null, null);

        // Assert
        assertEquals(2, responses.size());
        assertEquals(Task.TaskPriority.ALTA, responses.get(0).getPrioridad());
        assertEquals(Task.TaskPriority.BAJA, responses.get(1).getPrioridad());
    }

    @Test
    @DisplayName("CA-11: Marcar tarea como completada")
    void testToggleTaskComplete() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            assertTrue(task.getCompleted());
            return task;
        });

        // Act
        TaskResponse response = taskService.toggleComplete(1L);

        // Assert
        assertTrue(response.getCompleted());
        verify(taskRepository).save(argThat(task -> task.getCompleted()));
    }

    @Test
    @DisplayName("Seguridad: No permitir acceso a tareas de otro usuario")
    void testUnauthorizedAccessToTask() {
        // Arrange
        User otherUser = User.builder().id(2L).build();
        mockTask.setUser(otherUser);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(securityUtil.getCurrentUserId()).thenReturn(1L);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            taskService.getTask(1L);
        });
    }

    @Test
    @DisplayName("CA-11: Eliminar tarea")
    void testDeleteTask() {
        // Arrange
        when(taskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        doNothing().when(taskRepository).delete(any(Task.class));

        // Act
        taskService.deleteTask(1L);

        // Assert
        verify(taskRepository).delete(mockTask);
    }
}
