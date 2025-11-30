package com.tecsup.productivity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncTecsupRequest {
    @NotBlank(message = "El token TECSUP es requerido")
    private String token;
}