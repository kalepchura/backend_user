// ============================================
// 4. AuthController.java (ACTUALIZAR - Agregar 2 métodos)
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.CompleteRegisterRequest;
import com.tecsup.productivity.dto.request.LoginRequest;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.dto.request.ValidateRegisterRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.AuthResponse;
import com.tecsup.productivity.dto.response.ValidationResponse;
import com.tecsup.productivity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ✅ NUEVO - PASO 1: Validar datos sin crear usuario
    @PostMapping("/register/validate")
    public ResponseEntity<ApiResponse<ValidationResponse>> validateRegister(
            @Valid @RequestBody ValidateRegisterRequest request
    ) {
        log.info("🔍 Validando registro para: {}", request.getEmail());
        ValidationResponse response = authService.validateRegistration(request);
        return ResponseEntity.ok(
                ApiResponse.success("Validación exitosa. Continúa con las preferencias.", response)
        );
    }

    // ✅ NUEVO - PASO 2: Completar registro con preferencias
    @PostMapping("/register/complete")
    public ResponseEntity<ApiResponse<AuthResponse>> completeRegister(
            @Valid @RequestBody CompleteRegisterRequest request
    ) {
        log.info("✅ Completando registro para: {}", request.getEmail());
        AuthResponse response = authService.completeRegistration(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario registrado exitosamente", response));
    }

    // Mantener el endpoint original por compatibilidad
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Usuario registrado exitosamente", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Inicio de sesión exitoso", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(
                ApiResponse.success("Sesión cerrada exitosamente", null)
        );
    }
}
