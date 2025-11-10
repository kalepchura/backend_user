// ============================================
// UpdateHabitRequest.java - HU-9, CA-12
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
}