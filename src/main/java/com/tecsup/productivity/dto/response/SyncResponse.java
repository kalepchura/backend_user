package com.tecsup.productivity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResponse {

    private Integer eventosSincronizados;

    // ✅ NUEVO - Para tareas sincronizadas
    private Integer tareasSincronizadas;

    private String mensaje;

    // Opcional: listado de eventos (si lo necesitas)
    private List<EventResponse> eventos;

    // ✅ NUEVO - Listado de tareas (si lo necesitas)
    private List<TaskResponse> tareas;
}