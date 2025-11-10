package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Habit> findByUserIdAndTipoOrderByCreatedAtDesc(
            Long userId,
            Habit.HabitType tipo
    );
}
