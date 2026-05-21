package com.technokratos.pact.chat.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CallSignalRequest {
    private UUID chatId;
    private String type;
    private Object payload;
}
