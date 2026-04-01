package com.technokratos.pact.user.dto;

import com.technokratos.pact.user.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
        private UUID id;
        private String username;
        private String name;
        private User.Role role;
        private String avatarUrl;
        private String isEnabled;
        private String isVerified;
}
