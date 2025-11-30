package com.tecsup.productivity.dto.request;

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
    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "Mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es requerido")
    private String name;

    @NotNull(message = "El tipo de usuario es requerido")
    private User.UserType tipo;

    @NotNull(message = "Debe aceptar los términos")
    private Boolean acceptTerms;

    private String tecsupToken;

    @NotNull(message = "Las preferencias son requeridas")
    private Map<String, Object> preferences;
}