package com.technokratos.pact.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public static UserNotFoundException byId(UUID id) {
    return new UserNotFoundException("User not found with ID: %s".formatted(id));
  }

  public static UserNotFoundException byUsername(String username) {
    return new UserNotFoundException("User not found with username: %s".formatted(username));
  }
}