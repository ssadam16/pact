package com.technokratos.pact.chat.controller;

import com.technokratos.pact.chat.dto.CallSignalRequest;
import com.technokratos.pact.chat.service.ChatService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallSignalController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/call.signal")
    public void signal(@Payload CallSignalRequest request, Principal principal) {
        UUID currentUserId = extractUserId(principal);

        String recipientUsername = chatService.getRecipientUsername(request.getChatId(), currentUserId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("chatId", request.getChatId());
        payload.put("type", request.getType());
        payload.put("payload", request.getPayload());
        payload.put("fromUserId", currentUserId);

        messagingTemplate.convertAndSendToUser(recipientUsername, "/queue/call", payload);
    }

    private UUID extractUserId(Principal principal) {
        if (principal == null) {
            throw new AuthenticationServiceException("User not authenticated");
        }
        Authentication auth = (Authentication) principal;
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return userDetails.getId();
    }
}
