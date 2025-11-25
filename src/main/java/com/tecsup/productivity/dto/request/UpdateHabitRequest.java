// ============================================
// UpdateHabitRequest.java - CORREGIDO
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.Habit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHabitRequest {

    private String nombre;

    private Habit.HabitType tipo;

    private Integer metaDiaria;

    // ✅ NUEVO - Permitir activar/desactivar hábitos
    private Boolean activo;
}