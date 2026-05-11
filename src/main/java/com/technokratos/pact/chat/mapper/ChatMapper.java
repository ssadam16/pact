package com.technokratos.pact.chat.mapper;

import com.technokratos.pact.chat.dto.ChatResponse;
import com.technokratos.pact.chat.model.Chat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "interlocutor", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    ChatResponse toResponse(Chat chat);
}