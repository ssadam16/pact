package com.technokratos.pact.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    @GetMapping("/list")
    public String chatsPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "chat/chats";
    }

    @GetMapping("/{chatId}")
    public String chatRoomPage(@PathVariable UUID chatId, Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("chatId", chatId);
        return "chat/room";
    }

    @GetMapping("/new")
    public String newChatPage(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        return "chat/new-chat";
    }
}