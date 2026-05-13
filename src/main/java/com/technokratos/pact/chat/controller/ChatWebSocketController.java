package com.technokratos.pact.chat.controller;

import com.technokratos.pact.chat.dto.*;
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
import java.util.*;

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

        chatService.sendMessage(request.getChatId(), request, currentUserId);
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

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload DeleteMessageRequest request,
                              Principal principal) {
        UUID currentUserId = extractUserId(principal);
        chatService.deleteMessage(request.getMessageId(), currentUserId);

        Map<String, Object> deletePayload = new HashMap<>();
        deletePayload.put("action", "DELETE");
        deletePayload.put("messageId", request.getMessageId());
        deletePayload.put("content", "Сообщение удалено");
        deletePayload.put("status", "DELETED");

        messagingTemplate.convertAndSend("/topic/chat." + request.getChatId(), Optional.of(deletePayload));
    }

    @MessageMapping("/chat.edit")
    public void editMessage(@Payload EditMessageRequest request,
                            Principal principal) {
        UUID currentUserId = extractUserId(principal);
        MessageResponse edited = chatService.editMessage(request.getMessageId(), request.getContent(), currentUserId);

        Map<String, Object> editPayload = new HashMap<>();
        editPayload.put("action", "EDIT");
        editPayload.put("message", edited);

        messagingTemplate.convertAndSend("/topic/chat." + request.getChatId(), Optional.of(editPayload));
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