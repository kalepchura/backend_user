package com.tecsup.productivity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_summaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}),
        indexes = @Index(name = "idx_user_date", columnList = "user_id, date")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    // ============================================
    // TAREAS
    // ============================================
    @Column(name = "total_tasks")
    @Builder.Default
    private Integer totalTasks = 0;

    @Column(name = "completed_tasks")
    @Builder.Default
    private Integer completedTasks = 0;

    // ============================================
    // HÁBITOS
    // ============================================
    @Column(name = "total_habits")
    @Builder.Default
    private Integer totalHabits = 0;

    @Column(name = "completed_habits")
    @Builder.Default
    private Integer completedHabits = 0;

    // ============================================
    // PROGRESO GENERAL
    // ============================================
    @Column(name = "progress_percentage")
    @Builder.Default
    private Integer progressPercentage = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============================================
    // HELPER METHOD
    // ============================================
    public void calculateProgress() {
        int total = totalTasks + totalHabits;
        int completed = completedTasks + completedHabits;

        this.progressPercentage = (total > 0)
                ? Math.round((completed * 100.0f) / total)
                : 0;
    }
}