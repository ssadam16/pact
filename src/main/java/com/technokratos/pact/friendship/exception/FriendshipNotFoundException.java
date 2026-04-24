package com.technokratos.pact.friendship.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FriendshipNotFoundException extends RuntimeException {

    public FriendshipNotFoundException(String message) {
        super(message);
    }

    public FriendshipNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static FriendshipNotFoundException byId(UUID id) {
        return new FriendshipNotFoundException("Friendship not found with ID: %s".formatted(id));
    }

    public static FriendshipNotFoundException between(UUID userId1, UUID userId2) {
        return new FriendshipNotFoundException("Friendship not found between user1 (ID=%s) and user2 (ID=%s)".formatted(userId1, userId2));
    }
}