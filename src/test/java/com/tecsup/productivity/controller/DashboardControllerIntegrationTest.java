package com.tecsup.productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.entity.*;
import com.tecsup.productivity.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar base de datos
        taskRepository.deleteAll();
        habitRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Crear usuario de prueba
        testUser = User.builder()
                .email("dashboard@tecsup.edu.pe")
                .password(passwordEncoder.encode("123456"))
                .name("Dashboard User")
                .tipo(User.UserType.STUDENT)
                .preferences(new HashMap<>())
                .build();
        testUser = userRepository.save(testUser);

        // Obtener token
        RegisterRequest request = RegisterRequest.builder()
                .email("dashboard@tecsup.edu.pe")
                .password("123456")
                .name("Dashboard User")
                .tipo(User.UserType.STUDENT)
                .acceptTerms(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        authToken = objectMapper.readTree(response).get("data").get("token").asText();
    }

    @Test
    @DisplayName("HU-4, CA-07: GET /api/dashboard - Vista Home muestra tareas y hábitos del día")
    void testGetDashboard() throws Exception {
        // Arrange - Crear datos de prueba
        LocalDate today = LocalDate.now();

        // Tarea pendiente
        Task task = Task.builder()
                .user(testUser)
                .titulo("Tarea Pendiente")
                .prioridad(Task.TaskPriority.ALTA)
                .completed(false)
                .build();
        taskRepository.save(task);

        // Hábito
        Habit habit = Habit.builder()
                .user(testUser)
                .nombre("Ejercicio")
                .tipo(Habit.HabitType.EJERCICIO)
                .metaDiaria(30)
                .build();
        habitRepository.save(habit);

        // Evento de hoy
        Event event = Event.builder()
                .user(testUser)
                .titulo("Reunión")
                .fecha(today)
                .hora(LocalTime.of(10, 0))
                .categoria(Event.EventCategory.PERSONAL)
                .sincronizadoTecsup(false)
                .build();
        eventRepository.save(event);

        // Act & Assert
        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tareasPendientes").isArray())
                .andExpect(jsonPath("$.data.tareasPendientes[0].titulo").value("Tarea Pendiente"))
                .andExpect(jsonPath("$.data.habitosHoy").isArray())
                .andExpect(jsonPath("$.data.habitosHoy[0].habit.nombre").value("Ejercicio"))
                .andExpect(jsonPath("$.data.eventosHoy").isArray())
                .andExpect(jsonPath("$.data.eventosHoy[0].titulo").value("Reunión"));
    }

    @Test
    @DisplayName("CA-07: Dashboard sin autenticación retorna 403")
    void testDashboardUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }
}