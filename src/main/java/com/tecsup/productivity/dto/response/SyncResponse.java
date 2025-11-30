package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResponse {
    private Integer eventosSincronizados;
    private Integer tareasSincronizadas;
    private String mensaje;
}