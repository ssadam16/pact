package com.technokratos.pact.game.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CannotLoadSteamSpyGamesException extends RuntimeException {
    public CannotLoadSteamSpyGamesException(String message) {
        super(message);
    }

    public CannotLoadSteamSpyGamesException(String message, Throwable cause) {
        super(message, cause);
    }
}
