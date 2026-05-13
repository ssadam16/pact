package com.technokratos.pact.chat.repository;

import com.technokratos.pact.chat.model.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c FROM Chat c WHERE c.firstUser.id = :userId OR c.secondUser.id = :userId")
    Page<Chat> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

    Optional<Chat> findByFirstUserIdAndSecondUserId(UUID firstUserId, UUID secondUserId);

    @Query("SELECT c FROM Chat c WHERE " +
            "(c.firstUser.id = :userA AND c.secondUser.id = :userB) OR " +
            "(c.firstUser.id = :userB AND c.secondUser.id = :userA)")
    Optional<Chat> findChatBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chat.id = :chatId AND m.author.id != :userId AND m.readAt IS NULL")
    long countUnreadMessages(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Chat c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :chatId")
    void updateLastActivity(@Param("chatId") UUID chatId);
}