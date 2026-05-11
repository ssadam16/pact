package com.technokratos.pact.chat.repository;

import com.technokratos.pact.chat.model.ChatMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMediaRepository extends JpaRepository<ChatMedia, UUID> {
}
