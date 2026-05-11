package com.technokratos.pact.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class MarkReadRequest {

    @NotNull(message = "ID чата обязателен")
    private UUID chatId;

    @NotNull(message = "ID сообщения обязательно")
    private UUID messageId;
}