package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.dto.request.UpdateProfileRequest;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("chatEnabled", true);
        preferences.put("darkMode", false);
        preferences.put("notifications", true);

        mockUser = User.builder()
                .id(1L)
                .email("test@tecsup.edu.pe")
                .name("Test User")
                .tipo(User.UserType.STUDENT)
                .tecsupToken(null)
                .preferences(preferences)
                .build();
    }

    @Test
    @DisplayName("Obtener perfil del usuario actual")
    void testGetCurrentUser() {
        // Arrange
        when(securityUtil.getCurrentUser()).thenReturn(mockUser);

        // Act
        UserResponse response = userService.getCurrentUser();

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("test@tecsup.edu.pe", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals(User.UserType.STUDENT, response.getTipo());
        assertTrue((Boolean) response.getPreferences().get("chatEnabled"));

        verify(securityUtil).getCurrentUser();
    }

    @Test
    @DisplayName("HU-3, CA-03: Actualizar nombre del perfil")
    void testUpdateProfileName() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Juan Carlos Pérez")
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("Juan Carlos Pérez", user.getName());
            return user;
        });

        // Act
        UserResponse response = userService.updateProfile(request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(argThat(user ->
                "Juan Carlos Pérez".equals(user.getName())
        ));
    }

    @Test
    @DisplayName("HU-3, CA-03: Actualizar email con validación de duplicados")
    void testUpdateProfileEmail() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .email("newemail@tecsup.edu.pe")
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.existsByEmail("newemail@tecsup.edu.pe")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        UserResponse response = userService.updateProfile(request);

        // Assert
        assertNotNull(response);
        verify(userRepository).existsByEmail("newemail@tecsup.edu.pe");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("CA-03: Validación - Email duplicado debe fallar")
    void testUpdateProfileDuplicateEmail() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .email("existing@tecsup.edu.pe")
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.existsByEmail("existing@tecsup.edu.pe")).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> userService.updateProfile(request)
        );

        assertEquals("El email ya está en uso", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-3, CA-03: Actualizar tipo de usuario")
    void testUpdateProfileTipo() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .tipo(User.UserType.GENERAL)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals(User.UserType.GENERAL, user.getTipo());
            return user;
        });

        // Act
        UserResponse response = userService.updateProfile(request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(argThat(user ->
                User.UserType.GENERAL.equals(user.getTipo())
        ));
    }

    @Test
    @DisplayName("HU-3, CA-03: Actualizar token TECSUP")
    void testUpdateProfileTecsupToken() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .tecsupToken("TEC2025XYZ789")
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("TEC2025XYZ789", user.getTecsupToken());
            return user;
        });

        // Act
        UserResponse response = userService.updateProfile(request);

        // Assert
        assertNotNull(response);
        verify(userRepository).save(argThat(user ->
                "TEC2025XYZ789".equals(user.getTecsupToken())
        ));
    }

    @Test
    @DisplayName("HU-11, CA-16: Actualizar preferencia chatEnabled")
    void testUpdatePreferencesChatEnabled() {
        // Arrange
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .chatEnabled(false)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertFalse((Boolean) user.getPreferences().get("chatEnabled"));
            return user;
        });

        // Act
        UserResponse response = userService.updatePreferences(request);

        // Assert
        assertNotNull(response);
        assertFalse((Boolean) response.getPreferences().get("chatEnabled"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("CA-02, CA-16: Actualizar preferencia darkMode")
    void testUpdatePreferencesDarkMode() {
        // Arrange
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .darkMode(true)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertTrue((Boolean) user.getPreferences().get("darkMode"));
            return user;
        });

        // Act
        UserResponse response = userService.updatePreferences(request);

        // Assert
        assertNotNull(response);
        assertTrue((Boolean) response.getPreferences().get("darkMode"));
    }

    @Test
    @DisplayName("CA-16: Actualizar múltiples preferencias simultáneamente")
    void testUpdateMultiplePreferences() {
        // Arrange
        UpdatePreferencesRequest request = UpdatePreferencesRequest.builder()
                .chatEnabled(false)
                .darkMode(true)
                .notifications(false)
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertFalse((Boolean) user.getPreferences().get("chatEnabled"));
            assertTrue((Boolean) user.getPreferences().get("darkMode"));
            assertFalse((Boolean) user.getPreferences().get("notifications"));
            return user;
        });

        // Act
        UserResponse response = userService.updatePreferences(request);

        // Assert
        assertNotNull(response);
        assertFalse((Boolean) response.getPreferences().get("chatEnabled"));
        assertTrue((Boolean) response.getPreferences().get("darkMode"));
        assertFalse((Boolean) response.getPreferences().get("notifications"));
    }

    @Test
    @DisplayName("CA-03: Cambios persistentes en DB")
    void testProfileChangesPersistence() {
        // Arrange
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Updated Name")
                .email("updated@tecsup.edu.pe")
                .tipo(User.UserType.GENERAL)
                .tecsupToken("NEWTOKEN123")
                .build();

        when(securityUtil.getCurrentUser()).thenReturn(mockUser);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        userService.updateProfile(request);

        // Assert - Verificar que save() fue llamado con cambios
        verify(userRepository).save(argThat(user ->
                "Updated Name".equals(user.getName()) &&
                        "updated@tecsup.edu.pe".equals(user.getEmail()) &&
                        User.UserType.GENERAL.equals(user.getTipo()) &&
                        "NEWTOKEN123".equals(user.getTecsupToken())
        ));
    }
}