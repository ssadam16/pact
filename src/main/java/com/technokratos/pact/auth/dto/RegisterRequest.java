package com.technokratos.pact.auth.dto;

import com.technokratos.pact.auth.annotation.UniqueUser;

@UniqueUser
public record RegisterRequest(
        String username,
        String name,
        String email,
        String password
) {
}
