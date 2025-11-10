package com.tecsup.productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.entity.User;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        RegisterRequest request = RegisterRequest.builder()
                .email("chat@tecsup.edu.pe")
                .password("123456")
                .name("Chat User")
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
    @DisplayName("HU-10, CA-15: POST /api/chat - Enviar mensaje y recibir respuesta")
    void testSendMessage() throws Exception {
        ChatMessageRequest request = new ChatMessageRequest("Hola");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mensaje").value("Hola"))
                .andExpect(jsonPath("$.data.respuesta").exists())
                .andExpect(jsonPath("$.data.respuesta", containsString("Chat User")));
    }

    @Test
    @DisplayName("CA-15: Chatbot responde a pregunta sobre tareas")
    void testChatbotRespondsToTasksQuestion() throws Exception {
        ChatMessageRequest request = new ChatMessageRequest("¿Cuáles son mis tareas pendientes?");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.respuesta", containsString("tareas")));
    }

    @Test
    @DisplayName("HU-11, CA-16: Chat deshabilitado retorna error")
    void testChatDisabledReturnsError() throws Exception {
        // Deshabilitar chat
        UpdatePreferencesRequest prefRequest = UpdatePreferencesRequest.builder()
                .chatEnabled(false)
                .build();

        mockMvc.perform(put("/api/users/me/preferences")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prefRequest)));

        // Intentar enviar mensaje
        ChatMessageRequest chatRequest = new ChatMessageRequest("Hola");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("deshabilitado")));
    }

    @Test
    @DisplayName("HU-10: GET /api/chat/history - Obtener historial")
    void testGetChatHistory() throws Exception {
        // Enviar algunos mensajes primero
        ChatMessageRequest msg1 = new ChatMessageRequest("Mensaje 1");
        ChatMessageRequest msg2 = new ChatMessageRequest("Mensaje 2");

        mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg1)));

        mockMvc.perform(post("/api/chat")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg2)));

        // Obtener historial
        mockMvc.perform(get("/api/chat/history?limit=10")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }
}