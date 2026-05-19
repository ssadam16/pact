package com.technokratos.pact.chat.repository;

import com.technokratos.pact.chat.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByChatIdOrderByCreatedAtAsc(UUID chatId, Pageable pageable);
    Page<ChatMessage> findByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable pageable);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readAt = CURRENT_TIMESTAMP, m.status = 'READ' " +
            "WHERE m.chat.id = :chatId AND m.author.id != :userId AND m.readAt IS NULL")
    int markMessagesAsRead(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chat.id = :chatId AND m.author.id != :userId AND m.readAt IS NULL")
    long countUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    List<ChatMessage> findByChatIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID chatId, LocalDateTime after);

    Optional<ChatMessage> findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);

    @Query("SELECT m FROM ChatMessage m WHERE m.chat.id = :chatId AND m.author.id != :userId AND m.readAt IS NULL ORDER BY m.createdAt DESC")
    Optional<ChatMessage> findLastUnreadMessage(@Param("chatId") UUID chatId, @Param("userId") UUID userId);
}