package com.tecsup.productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.productivity.dto.request.CreateEventRequest;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.dto.request.SyncTecsupRequest;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Registrar y obtener token
        RegisterRequest request = RegisterRequest.builder()
                .email("events@tecsup.edu.pe")
                .password("123456")
                .name("Events User")
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
    @DisplayName("HU-6, CA-09: CRUD eventos completo con categorías")
    void testCreateEvent() throws Exception {
        // Arrange
        CreateEventRequest request = CreateEventRequest.builder()
                .titulo("Examen Final")
                .fecha(LocalDate.now().plusDays(7))
                .hora(LocalTime.of(14, 0))
                .categoria(Event.EventCategory.EXAMEN)
                .descripcion("Examen de Spring Boot")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titulo").value("Examen Final"))
                .andExpect(jsonPath("$.data.categoria").value("EXAMEN"));
    }

    @Test
    @DisplayName("CA-09: Filtrar eventos por categoría")
    void testGetEventsByCategory() throws Exception {
        // Arrange - Crear eventos de diferentes categorías
        CreateEventRequest clase = CreateEventRequest.builder()
                .titulo("Clase de Java")
                .fecha(LocalDate.now())
                .categoria(Event.EventCategory.CLASE)
                .build();

        CreateEventRequest examen = CreateEventRequest.builder()
                .titulo("Examen")
                .fecha(LocalDate.now())
                .categoria(Event.EventCategory.EXAMEN)
                .build();

        mockMvc.perform(post("/api/events")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clase)));

        mockMvc.perform(post("/api/events")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(examen)));

        // Act & Assert - Filtrar solo CLASE
        mockMvc.perform(get("/api/events?categoria=CLASE")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].categoria").value("CLASE"));
    }

    @Test
    @DisplayName("HU-7, CA-10: Sincronización TECSUP automática")
    void testSyncTecsup() throws Exception {
        // Arrange
        SyncTecsupRequest request = new SyncTecsupRequest("TEC2025ABC123");

        // Act & Assert
        mockMvc.perform(post("/api/sync/tecsup")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventosSincronizados").isNumber())
                .andExpect(jsonPath("$.data.eventos").isArray())
                .andExpect(jsonPath("$.data.eventos[0].sincronizadoTecsup").value(true));
    }
}