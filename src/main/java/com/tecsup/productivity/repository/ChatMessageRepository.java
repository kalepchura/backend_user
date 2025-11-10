package com.tecsup.productivity.repository;

import com.tecsup.productivity.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.user.id = :userId " +
            "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByUser(
            @Param("userId") Long userId,
            Pageable pageable
    );
}