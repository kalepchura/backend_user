package com.tecsup.productivity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "habit_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "fecha"}),
        indexes = @Index(name = "idx_habit_fecha", columnList = "habit_id, fecha")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    @JsonIgnore
    private Habit habit;

    @Column(nullable = false)
    private LocalDate fecha;

    @Builder.Default
    private Boolean completado = false;

    private Integer valor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}