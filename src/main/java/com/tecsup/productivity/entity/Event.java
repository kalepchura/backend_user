package com.tecsup.productivity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_user_fecha", columnList = "user_id, fecha"),
        @Index(name = "idx_user_categoria", columnList = "user_id, categoria"),
        @Index(name = "idx_event_user_source", columnList = "user_id, source") // ✅ Único
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 250)
    private String titulo;

    @Column(nullable = false)
    private LocalDate fecha;

    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory categoria;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 200)
    private String curso;

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

    public enum EventCategory {
        CLASE, EXAMEN, PERSONAL
    }
}