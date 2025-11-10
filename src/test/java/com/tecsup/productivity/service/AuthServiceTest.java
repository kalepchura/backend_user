package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.LoginRequest;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.dto.response.AuthResponse;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .email("test@tecsup.edu.pe")
                .password("123456")
                .name("Test User")
                .tipo(User.UserType.STUDENT)
                .acceptTerms(true)
                .build();

        validLoginRequest = new LoginRequest("test@tecsup.edu.pe", "123456");

        mockUser = User.builder()
                .id(1L)
                .email("test@tecsup.edu.pe")
                .password("encodedPassword")
                .name("Test User")
                .tipo(User.UserType.STUDENT)
                .preferences(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("HU-1, CA-01: Registro exitoso con validación y selector de tipo")
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("mock-jwt-token");

        // Act
        AuthResponse response = authService.register(validRegisterRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("test@tecsup.edu.pe", response.getUser().getEmail());
        assertEquals(User.UserType.STUDENT, response.getUser().getTipo());

        verify(userRepository).existsByEmail("test@tecsup.edu.pe");
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken("test@tecsup.edu.pe", 1L);
    }

    @Test
    @DisplayName("CA-01: Validación - Email duplicado debe fallar")
    void testRegisterFailDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(validRegisterRequest)
        );

        assertEquals("El email ya está registrado", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("CA-01: Validación - Términos no aceptados debe fallar")
    void testRegisterFailTermsNotAccepted() {
        // Arrange
        validRegisterRequest.setAcceptTerms(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(validRegisterRequest)
        );

        assertEquals("Debe aceptar los términos y condiciones", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-2, CA-02: Login funcional; token persistente")
    void testLoginSuccess() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("mock-jwt-token");

        // Act
        AuthResponse response = authService.login(validLoginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("test@tecsup.edu.pe", response.getUser().getEmail());

        verify(authenticationManager).authenticate(any());
        verify(userRepository).findByEmail("test@tecsup.edu.pe");
    }

    @Test
    @DisplayName("CA-02: Preferencias por defecto al registrar")
    void testRegisterCreatesDefaultPreferences() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertNotNull(user.getPreferences());
            assertEquals(true, user.getPreferences().get("chatEnabled"));
            assertEquals(false, user.getPreferences().get("darkMode"));
            assertEquals(true, user.getPreferences().get("notifications"));
            return mockUser;
        });
        when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("token");

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository).save(argThat(user ->
                user.getPreferences().containsKey("chatEnabled") &&
                        user.getPreferences().containsKey("darkMode") &&
                        user.getPreferences().containsKey("notifications")
        ));
    }
}