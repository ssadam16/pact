package com.technokratos.pact.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DeleteMessageRequest {

    @NotNull
    private UUID chatId;

    @NotNull
    private UUID messageId;
}
