package com.technokratos.pact.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ChatCreateRequest {

    @NotNull(message = "ID второго пользователя обязателен")
    private UUID secondUserId;
}