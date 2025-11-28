package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.dto.request.UpdateProfileRequest;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final PasswordEncoder passwordEncoder;

    // ============================================
    // PANTALLA PERFIL - OBTENER DATOS
    // ============================================

    /**
     * Obtener perfil del usuario actual
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = securityUtil.getCurrentUser();
        log.info("👤 Obteniendo perfil de: {}", user.getEmail());
        return mapToUserResponse(user);
    }

    // ============================================
    // ACTUALIZAR PERFIL
    // ============================================

    /**
     * Actualizar información del perfil (nombre)
     */
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = securityUtil.getCurrentUser();

        log.info("✏️ Actualizando perfil de: {}", user.getEmail());

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        user = userRepository.save(user);
        log.info("✅ Perfil actualizado");

        return mapToUserResponse(user);
    }

    /**
     * Cambiar contraseña
     */
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User user = securityUtil.getCurrentUser();

        log.info("🔐 Cambiando contraseña de: {}", user.getEmail());

        // Verificar contraseña actual
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("La contraseña actual es incorrecta");
        }

        // Validar nueva contraseña
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("La nueva contraseña debe tener al menos 6 caracteres");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("✅ Contraseña cambiada exitosamente");
    }

    // ============================================
    // PREFERENCIAS
    // ============================================

    /**
     * Actualizar preferencias del usuario
     */
    @Transactional
    public UserResponse updatePreferences(UpdatePreferencesRequest request) {
        User user = securityUtil.getCurrentUser();

        log.info("⚙️ Actualizando preferencias de: {}", user.getEmail());

        Map<String, Object> preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new HashMap<>();
        }

        // Actualizar preferencias individuales
        if (request.getChatEnabled() != null) {
            preferences.put("chatEnabled", request.getChatEnabled());
            log.info("  - Chat IA: {}", request.getChatEnabled());
        }

        if (request.getDarkMode() != null) {
            preferences.put("darkMode", request.getDarkMode());
            log.info("  - Dark Mode: {}", request.getDarkMode());
        }

        if (request.getNotifications() != null) {
            preferences.put("notifications", request.getNotifications());
            log.info("  - Notificaciones: {}", request.getNotifications());
        }

        user.setPreferences(preferences);
        user = userRepository.save(user);

        log.info("✅ Preferencias actualizadas");
        return mapToUserResponse(user);
    }

    /**
     * Obtener solo las preferencias
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPreferences() {
        User user = securityUtil.getCurrentUser();
        return user.getPreferences() != null ? user.getPreferences() : new HashMap<>();
    }

    /**
     * Activar/Desactivar una preferencia específica
     */
    @Transactional
    public UserResponse togglePreference(String key) {
        User user = securityUtil.getCurrentUser();

        log.info("🔄 Toggle preferencia '{}' para: {}", key, user.getEmail());

        Map<String, Object> preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new HashMap<>();
        }

        // Toggle boolean
        Boolean currentValue = (Boolean) preferences.getOrDefault(key, false);
        preferences.put(key, !currentValue);

        user.setPreferences(preferences);
        user = userRepository.save(user);

        log.info("✅ Preferencia '{}' cambiada a: {}", key, !currentValue);
        return mapToUserResponse(user);
    }

    // ============================================
    // ESTADÍSTICAS DEL USUARIO (opcional para PERFIL)
    // ============================================

    /**
     * Obtener estadísticas generales del usuario
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        User user = securityUtil.getCurrentUser();

        log.info("📊 Obteniendo estadísticas de: {}", user.getEmail());

        // TODO: Implementar contadores reales cuando lo necesites
        Map<String, Object> stats = new HashMap<>();
        stats.put("email", user.getEmail());
        stats.put("name", user.getName());
        stats.put("tipo", user.getTipo());
        stats.put("createdAt", user.getCreatedAt());
        stats.put("hasTecsupSync", user.getTecsupToken() != null);

        return stats;
    }

    // ============================================
    // MAPPER
    // ============================================

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .tipo(user.getTipo())
                .tecsupToken(user.getTecsupToken())
                .preferences(user.getPreferences())
                .createdAt(user.getCreatedAt())
                .build();
    }
}