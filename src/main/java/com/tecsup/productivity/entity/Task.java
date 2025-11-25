package com.tecsup.productivity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_user_completed", columnList = "user_id, completed"),
        @Index(name = "idx_user_prioridad", columnList = "user_id, prioridad"),
        @Index(name = "idx_task_user_source", columnList = "user_id, source") // ✅ RENOMBRADO
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority prioridad;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Builder.Default
    private Boolean completed = false;

    // ============================================
    // ✅ CAMPOS NUEVOS - Hibernate los creará automáticamente
    // ============================================

    @Column(length = 20)
    @Builder.Default
    private String source = "user"; // "user" o "tecsup"

    @Column(name = "tecsup_external_id", length = 100)
    private String tecsupExternalId; // ID remoto de Canvas

    @Column(name = "sincronizado_tecsup")
    @Builder.Default
    private Boolean sincronizadoTecsup = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // ✅ NUEVO

    public enum TaskPriority {
        ALTA, MEDIA, BAJA
    }
}