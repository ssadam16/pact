package com.technokratos.pact.friendship.dto;

import com.technokratos.pact.user.dto.UserShortProfileResponse;

import java.util.UUID;

public record FriendshipIncomeRequest(
        UUID id,
        UserShortProfileResponse who
) {
}
