// ============================================
// SyncResponse.java - HU-7, CA-10
// ============================================
package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncResponse {
    private Integer eventosSincronizados;
    private List<EventResponse> eventos;
}