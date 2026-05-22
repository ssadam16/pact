package com.technokratos.pact.game.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class GameAlreadyExistsException extends RuntimeException {

    public GameAlreadyExistsException(String message) {
        super(message);
    }

    public static GameAlreadyExistsException byName(String name) {
        return new GameAlreadyExistsException("Game already exists with name: " + name);
    }
}