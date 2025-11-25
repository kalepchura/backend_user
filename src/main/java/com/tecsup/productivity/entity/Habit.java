package com.tecsup.productivity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "habits", indexes = {
        @Index(name = "idx_user_activo", columnList = "user_id, activo") // ✅ NUEVO
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HabitType tipo;

    @Column(name = "meta_diaria")
    private Integer metaDiaria; // ✅ Ya NO es obligatorio (para comidas)

    // ============================================
    // ✅ CAMPOS NUEVOS - Hibernate los creará automáticamente
    // ============================================

    @Column(name = "es_comida")
    @Builder.Default
    private Boolean esComida = false; // ✅ NUEVO - true para DESAYUNO, ALMUERZO, CENA

    @Builder.Default
    private Boolean activo = true; // ✅ NUEVO - para habilitar/deshabilitar hábitos

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum HabitType {
        SUEÑO, EJERCICIO, AGUA, DESAYUNO, ALMUERZO, CENA, OTRO // ✅ AGREGADOS: DESAYUNO, ALMUERZO, CENA
    }
}