package com.technokratos.pact.chat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MessageNotFoundException extends RuntimeException {

    public MessageNotFoundException(String message) {
        super(message);
    }

    public MessageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static MessageNotFoundException byId(UUID id) {
        return new MessageNotFoundException("Message not found with ID: %s".formatted(id));
    }
}