// ============================================
// RegisterRequest.java - HU-1, CA-01
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "Mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 3, message = "Mínimo 3 caracteres")
    private String name;

    @NotNull(message = "El tipo de usuario es requerido")
    private User.UserType tipo;

    @NotNull(message = "Debe aceptar los términos")
    private Boolean acceptTerms;
}

