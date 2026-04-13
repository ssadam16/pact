package com.technokratos.pact.user.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserShortProfileResponse {
        private UUID id;
        private String username;
        private String avatarUrl;
}
