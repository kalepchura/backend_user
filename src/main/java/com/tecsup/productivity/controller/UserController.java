// ============================================
// UserController.java - EP-01
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.dto.request.UpdateProfileRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userService.updateProfile(request);
        return ResponseEntity.ok(
                ApiResponse.success("Perfil actualizado exitosamente", response)
        );
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<ApiResponse<UserResponse>> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        UserResponse response = userService.updatePreferences(request);
        return ResponseEntity.ok(
                ApiResponse.success("Preferencias actualizadas", response)
        );
    }
}
