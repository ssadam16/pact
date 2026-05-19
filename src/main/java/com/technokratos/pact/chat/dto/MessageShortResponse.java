package com.technokratos.pact.chat.dto;

import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class MessageShortResponse {
    private UUID id;
    private String content;
    private UserShortProfileResponse author;
    private LocalDateTime createdAt;
}
