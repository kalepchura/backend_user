package com.tecsup.productivity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHabitRequest {
    private String nombre;
    private Integer metaDiaria;
    private Boolean activo;
}