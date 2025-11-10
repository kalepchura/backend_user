package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateHabitRequest;
import com.tecsup.productivity.dto.request.LogHabitRequest;
import com.tecsup.productivity.dto.response.HabitProgressResponse;
import com.tecsup.productivity.dto.response.HabitResponse;
import com.tecsup.productivity.entity.Habit;
import com.tecsup.productivity.entity.HabitLog;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.repository.HabitLogRepository;
import com.tecsup.productivity.repository.HabitRepository;
import com.tecsup.productivity.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private HabitService habitService;

    private User mockUser;
    private Habit mockHabit;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@tecsup.edu.pe")
                .build();

        mockHabit = Habit.builder()
                .id(1L)
                .user(mockUser)
                .nombre("Dormir 8 horas")
                .tipo(Habit.HabitType.SUEÑO)
                .metaDiaria(8)
                .build();
    }

    @Test
    @DisplayName("HU-9, CA-12: Registrar hábito")
    void testCreateHabit() {
        // Arrange
        CreateHabitRequest request = CreateHabitRequest.builder()
                .nombre("Ejercicio diario")
                .tipo(Habit.HabitType.EJERCICIO)
                .metaDiaria(30)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(habitRepository.save(any(Habit.class))).thenReturn(mockHabit);

        // Act
        HabitResponse response = habitService.createHabit(request);

        // Assert
        assertNotNull(response);
        assertEquals("Dormir 8 horas", response.getNombre());
        assertEquals(Habit.HabitType.SUEÑO, response.getTipo());
        assertEquals(8, response.getMetaDiaria());

        verify(habitRepository).save(any(Habit.class));
    }

    @Test
    @DisplayName("CA-12: Registrar progreso diario")
    void testLogHabit() {
        // Arrange
        LogHabitRequest request = LogHabitRequest.builder()
                .habitId(1L)
                .fecha(LocalDate.now())
                .completado(true)
                .valor(8)
                .build();

        HabitLog mockLog = HabitLog.builder()
                .id(1L)
                .habit(mockHabit)
                .fecha(LocalDate.now())
                .completado(true)
                .valor(8)
                .build();

        when(habitRepository.findById(1L)).thenReturn(Optional.of(mockHabit));
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(habitLogRepository.findByHabitIdAndFecha(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(habitLogRepository.save(any(HabitLog.class))).thenReturn(mockLog);

        // Act
        var response = habitService.logHabit(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getCompletado());
        assertEquals(8, response.getValor());

        verify(habitLogRepository).save(any(HabitLog.class));
    }

    @Test
    @DisplayName("CA-12: Gráfico semanal visible y dinámico")
    void testGetWeeklyProgress() {
        // Arrange
        List<HabitLog> weeklyLogs = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            weeklyLogs.add(HabitLog.builder()
                    .id((long) i)
                    .habit(mockHabit)
                    .fecha(today.minusDays(6 - i))
                    .completado(i % 2 == 0)
                    .valor(7 + i % 2)
                    .build());
        }

        when(habitRepository.findById(1L)).thenReturn(Optional.of(mockHabit));
        when(securityUtil.getCurrentUserId()).thenReturn(1L);
        when(habitLogRepository.findByHabitIdAndFechaBetweenOrderByFechaAsc(
                anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(weeklyLogs);

        // Act
        HabitProgressResponse response = habitService.getHabitProgress(1L, 7);

        // Assert
        assertNotNull(response);
        assertEquals(7, response.getWeeklyLogs().size());
        assertEquals("Dormir 8 horas", response.getHabit().getNombre());

        // Verificar que los logs están ordenados por fecha
        for (int i = 0; i < 6; i++) {
            assertTrue(response.getWeeklyLogs().get(i).getFecha()
                    .isBefore(response.getWeeklyLogs().get(i + 1).getFecha()) ||
                    response.getWeeklyLogs().get(i).getFecha()
                            .isEqual(response.getWeeklyLogs().get(i + 1).getFecha()));
        }
    }
}