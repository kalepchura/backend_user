package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.dto.request.UpdateProfileRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ============================================
    // ENDPOINTS PARA PANTALLA PERFIL
    // ============================================

    /**
     * GET /api/user/profile
     *
     * Obtener datos del perfil del usuario actual
     *
     * Para: Pantalla PERFIL (cargar datos al abrir)
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        log.info("👤 [GET] /api/user/profile");

        UserResponse user = userService.getCurrentUserProfile();

        return ResponseEntity.ok(
                ApiResponse.success("Perfil obtenido", user)
        );
    }

    /**
     * PUT /api/user/profile
     *
     * Actualizar información del perfil (nombre)
     * Body: { "name": "Nuevo Nombre" }
     *
     * Para: Editar nombre en PERFIL
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("✏️ [PUT] /api/user/profile");

        UserResponse user = userService.updateProfile(request);

        return ResponseEntity.ok(
                ApiResponse.success("Perfil actualizado exitosamente", user)
        );
    }

    /**
     * POST /api/user/change-password
     *
     * Cambiar contraseña
     * Body: { "currentPassword": "...", "newPassword": "..." }
     *
     * Para: Botón "Cambiar contraseña" en PERFIL
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> request
    ) {
        log.info("🔐 [POST] /api/user/change-password");

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Contraseñas requeridas")
            );
        }

        userService.changePassword(currentPassword, newPassword);

        return ResponseEntity.ok(
                ApiResponse.success("Contraseña cambiada exitosamente", null)
        );
    }

    // ============================================
    // PREFERENCIAS
    // ============================================

    /**
     * GET /api/user/preferences
     *
     * Obtener preferencias del usuario
     *
     * Para: Sección de preferencias en PERFIL
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreferences() {
        log.info("⚙️ [GET] /api/user/preferences");

        Map<String, Object> preferences = userService.getPreferences();

        return ResponseEntity.ok(
                ApiResponse.success("Preferencias obtenidas", preferences)
        );
    }

    /**
     * PUT /api/user/preferences
     *
     * Actualizar preferencias
     * Body: {
     *   "chatEnabled": true,
     *   "darkMode": false,
     *   "notifications": true
     * }
     *
     * Para: Switches de preferencias en PERFIL
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<UserResponse>> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        log.info("⚙️ [PUT] /api/user/preferences");

        UserResponse user = userService.updatePreferences(request);

        return ResponseEntity.ok(
                ApiResponse.success("Preferencias actualizadas exitosamente", user)
        );
    }

    /**
     * PATCH /api/user/preferences/{key}/toggle
     *
     * Toggle de una preferencia específica
     * Ejemplo: PATCH /api/user/preferences/darkMode/toggle
     *
     * Para: Switch individual en PERFIL
     */
    @PatchMapping("/preferences/{key}/toggle")
    public ResponseEntity<ApiResponse<UserResponse>> togglePreference(
            @PathVariable String key
    ) {
        log.info("🔄 [PATCH] /api/user/preferences/{}/toggle", key);

        UserResponse user = userService.togglePreference(key);

        return ResponseEntity.ok(
                ApiResponse.success(
                        String.format("Preferencia '%s' actualizada", key),
                        user
                )
        );
    }

    /**
     * GET /api/user/stats
     *
     * Obtener estadísticas del usuario
     *
     * Para: Sección de estadísticas en PERFIL (opcional)
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats() {
        log.info("📊 [GET] /api/user/stats");

        Map<String, Object> stats = userService.getUserStats();

        return ResponseEntity.ok(
                ApiResponse.success("Estadísticas obtenidas", stats)
        );
    }
}