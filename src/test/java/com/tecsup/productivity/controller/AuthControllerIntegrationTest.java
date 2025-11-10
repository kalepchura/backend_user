package com.tecsup.productivity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.productivity.dto.request.LoginRequest;
import com.tecsup.productivity.dto.request.RegisterRequest;
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
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("HU-1, CA-01: POST /api/auth/register - Registro exitoso")
    void testRegisterSuccess() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("nuevo@tecsup.edu.pe")
                .password("123456")
                .name("Nuevo Usuario")
                .tipo(User.UserType.STUDENT)
                .acceptTerms(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario registrado exitosamente"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("nuevo@tecsup.edu.pe"))
                .andExpect(jsonPath("$.data.user.tipo").value("STUDENT"))
                .andExpect(jsonPath("$.data.user.preferences.chatEnabled").value(true))
                .andExpect(jsonPath("$.data.user.preferences.darkMode").value(false));
    }

    @Test
    @DisplayName("CA-01: Validación - Email duplicado retorna error 400")
    void testRegisterDuplicateEmail() throws Exception {
        // Arrange - Crear usuario existente
        RegisterRequest firstRequest = RegisterRequest.builder()
                .email("duplicado@tecsup.edu.pe")
                .password("123456")
                .name("Primer Usuario")
                .tipo(User.UserType.STUDENT)
                .acceptTerms(true)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // Act & Assert - Intentar registrar con mismo email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El email ya está registrado"));
    }

    @Test
    @DisplayName("CA-01: Validación - Campos requeridos")
    void testRegisterValidation() throws Exception {
        // Arrange - Request inválido
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .email("invalid-email")
                .password("123")
                .name("Ab")
                .tipo(User.UserType.STUDENT)
                .acceptTerms(true)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("HU-2, CA-02: POST /api/auth/login - Login exitoso con JWT")
    void testLoginSuccess() throws Exception {
        // Arrange - Registrar usuario primero
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("login@tecsup.edu.pe")
                .password("123456")
                .name("Usuario Login")
                .tipo(User.UserType.GENERAL)
                .acceptTerms(true)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Act - Login
        LoginRequest loginRequest = new LoginRequest("login@tecsup.edu.pe", "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("login@tecsup.edu.pe"));
    }

    @Test
    @DisplayName("CA-02: Login con credenciales inválidas retorna 401")
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest("noexiste@tecsup.edu.pe", "wrongpass");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}