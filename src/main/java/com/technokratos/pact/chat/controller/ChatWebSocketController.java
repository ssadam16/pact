package com.technokratos.pact.chat.controller;

import com.technokratos.pact.chat.dto.MarkReadRequest;
import com.technokratos.pact.chat.dto.MessageResponse;
import com.technokratos.pact.chat.dto.SendMessageRequest;
import com.technokratos.pact.chat.dto.TypingStatusRequest;
import com.technokratos.pact.chat.service.ChatService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid SendMessageRequest request,
                            Principal principal) {

        UUID currentUserId = extractUserId(principal);

        log.info("WebSocket send message from user: {} to chat: {}", currentUserId, request.getChatId());

        List<MessageResponse> messages = chatService.sendMessage(request.getChatId(), request, currentUserId);

        messages.forEach(m -> {
            messagingTemplate.convertAndSend("/topic/chat." + request.getChatId(), m);
        });
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload @Valid MarkReadRequest request,
                           Principal principal) {

        UUID currentUserId = extractUserId(principal);

        log.info("WebSocket mark read from user: {} in chat: {}", currentUserId, request.getChatId());

        chatService.markMessagesAsRead(request.getChatId(), request.getMessageId(), currentUserId);
    }

    @MessageMapping("/chat.typing")
    public void typingStatus(@Payload TypingStatusRequest request,
                             Principal principal) {

        UUID currentUserId = extractUserId(principal);

        log.debug("WebSocket typing status from user: {} in chat: {}, typing: {}",
                currentUserId, request.getChatId(), request.getIsTyping());

        chatService.sendTypingStatus(request.getChatId(), request.getIsTyping(), currentUserId);
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