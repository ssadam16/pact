package com.technokratos.pact.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class EditMessageRequest {

    @NotNull
    private UUID chatId;

    @NotNull
    private UUID messageId;

    @NotNull
    @Size(max = 4096)
    private String content;
}
