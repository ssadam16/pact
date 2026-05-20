package com.technokratos.pact.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileStatsResponse {
    private int articlesCount;
    private int commentsCount;
    private int likesCount;
    private long daysInCommunity;
}
