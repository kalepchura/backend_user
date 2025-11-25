package com.tecsup.productivity.dto.request;

// ============================================
// 2. CompleteRegisterRequest.java (DTO para Paso 2)
// ============================================


import com.tecsup.productivity.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteRegisterRequest {

    // Datos validados del Paso 1
    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    private String password;

    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El tipo de usuario es requerido")
    private User.UserType tipo;

    @NotNull(message = "Debe aceptar los términos")
    private Boolean acceptTerms;

    private String tecsupToken;

    // Preferencias del Paso 2
    @NotNull(message = "Las preferencias son requeridas")
    private Map<String, Object> preferences;
}

