// ============================================
// ChatMessageRequest.java - HU-10, CA-15
// ============================================
package com.tecsup.productivity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "El mensaje es requerido")
    private String mensaje;
}