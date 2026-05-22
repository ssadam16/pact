package com.technokratos.pact.game.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(String message) {
        super(message);
    }

    public static GameNotFoundException byId(UUID id) {
        return new GameNotFoundException("Game not found with ID: " + id);
    }
}