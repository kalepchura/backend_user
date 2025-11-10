package com.tecsup.productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.dto.request.UpdateProfileRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

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
                .email("user@tecsup.edu.pe")
                .password("123456")
                .name("Profile User")
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
    @DisplayName("GET /api/users/me - Obtener perfil actual")
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@tecsup.edu.pe"))
                .andExpect(jsonPath("$.data.name").value("Profile User"))
                .andExpect(jsonPath("$.data.tipo").value("STUDENT"));
    }

    @Test
    @DisplayName("HU-3, CA-03: PUT /api/users/me - Actualizar nombre")
    void testUpdateProfileName() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Nombre Actualizado")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Perfil actualizado exitosamente"))
                .andExpect(jsonPath("$.data.name").value("Nombre Actualizado"));
    }

    @Test
    @DisplayName("HU-3, CA-03: Actualizar email con validación")
    void testUpdateProfileEmail() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .email("newemail@tecsup.edu.pe")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("newemail@tecsup.edu.pe"));
    }

    @Test
    @DisplayName("HU-3, CA-03: Actualizar token TECSUP")
    void testUpdateTecsupToken() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .tecsupToken("TEC2025ABC123")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tecsupToken").value("TEC2025ABC123"));
    }

    @Test
    @DisplayName("HU-11, CA-16: PUT /api/users/me/preferences - Desactivar chatbot")
    void testUpdatePreferencesChatDisabled() throws Exception {
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .chatEnabled(false)
                .build();

        mockMvc.perform(put("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferences.chatEnabled").value(false));
    }

    @Test
    @DisplayName("CA-02, CA-16: Activar modo oscuro")
    void testUpdatePreferencesDarkMode() throws Exception {
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .darkMode(true)
                .build();

        mockMvc.perform(put("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferences.darkMode").value(true));
    }
}
