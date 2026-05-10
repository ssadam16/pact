package com.technokratos.pact.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatedUserSessionInfo {
    private String email;
    private String username;
    private String avatarUrl;
}
