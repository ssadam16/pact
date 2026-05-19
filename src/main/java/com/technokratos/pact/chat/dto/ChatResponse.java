package com.technokratos.pact.chat.dto;

import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ChatResponse {
    private UUID id;
    private UserShortProfileResponse interlocutor;
    private Long unreadCount;
    private MessageShortResponse lastMessage;
}
