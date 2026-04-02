package com.technokratos.pact.security.exception;

import org.springframework.security.core.AuthenticationException;

import java.util.UUID;

public class DisabledAccountException extends AuthenticationException {
    public DisabledAccountException(String message) {
        super(message);
    }

    public DisabledAccountException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DisabledAccountException byId(UUID id) {
        return new DisabledAccountException("Account is deleted with ID: %s".formatted(id));
    }

    public static DisabledAccountException byUsername(String username) {
        return new DisabledAccountException("Account is deleted with username: %s".formatted(username));
    }
}
