package com.technokratos.pact.chat.controller;

import com.technokratos.pact.chat.dto.ChatCreateRequest;
import com.technokratos.pact.chat.dto.ChatResponse;
import com.technokratos.pact.chat.dto.MessageResponse;
import com.technokratos.pact.chat.service.ChatService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@Valid @RequestBody ChatCreateRequest request,
                                                   @AuthenticationPrincipal UserDetailsImpl currentUser) {
        ChatResponse response = chatService.createChat(request, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ChatResponse>> getUserChats(@RequestParam(defaultValue = "0") int page,
                                                           @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<ChatResponse> chats = chatService.getUserChats(currentUser.getId(), page);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<Page<MessageResponse>> getChatMessages(@PathVariable UUID chatId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Page<MessageResponse> messages = chatService.getChatMessages(chatId, currentUser.getId(), page);

        // Mark all unread incoming messages as read when user opens the chat.
        // Handles offline scenario: sender gets READ notification via WebSocket
        // even if recipient was not connected when messages were sent.
        if (page == 0) {
            chatService.markAllMessagesAsRead(chatId, currentUser.getId());
        }

        return ResponseEntity.ok(messages);
    }
}