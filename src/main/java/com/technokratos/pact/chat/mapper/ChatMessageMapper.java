package com.technokratos.pact.chat.mapper;

import com.technokratos.pact.chat.dto.MessageResponse;
import com.technokratos.pact.chat.dto.MessageShortResponse;
import com.technokratos.pact.chat.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "replyToMessage", ignore = true)
    MessageResponse toResponse(ChatMessage message);

    MessageShortResponse toShortResponse(ChatMessage message);
}