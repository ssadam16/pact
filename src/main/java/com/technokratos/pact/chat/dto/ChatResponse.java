package com.technokratos.pact.chat.dto;

import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatResponse {
    private UUID id;
    private UserShortProfileResponse firstUser;
    private UserShortProfileResponse secondUser;
    private MessageShortResponse lastMessage;
}
