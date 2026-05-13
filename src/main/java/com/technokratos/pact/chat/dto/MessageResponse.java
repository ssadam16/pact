package com.technokratos.pact.chat.dto;

import com.technokratos.pact.chat.model.ChatMessage;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class MessageResponse {
    private UUID id;
    private String content;
    private UserShortProfileResponse author;
    private ChatMessage.MessageStatus status;
    private MessageShortResponse replyToMessage;
    private List<MediaResponse> mediaList;
    private LocalDateTime createdAt;
    private Boolean isEdited;
}