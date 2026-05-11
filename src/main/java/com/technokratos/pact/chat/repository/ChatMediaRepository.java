package com.technokratos.pact.chat.repository;

import com.technokratos.pact.chat.model.ChatMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMediaRepository extends JpaRepository<ChatMedia, UUID> {
    List<ChatMedia> findByMessageIdOrderByOrderNumAsc(UUID messageId);
    void deleteByMessageId(UUID messageId);
}