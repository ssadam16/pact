package com.technokratos.pact.chat.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class TypingStatusRequest {
    private UUID chatId;
    private Boolean isTyping;
}