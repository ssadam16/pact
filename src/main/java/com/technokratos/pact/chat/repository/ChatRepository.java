package com.technokratos.pact.chat.repository;

import com.technokratos.pact.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
}
