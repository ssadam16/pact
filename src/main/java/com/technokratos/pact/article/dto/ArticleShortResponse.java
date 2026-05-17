package com.technokratos.pact.article.dto;

import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class ArticleShortResponse {
    private UUID id;
    private String title;
    private UserShortProfileResponse author;
    private Set<GameResponse> games;
    private LocalDateTime createdAt;
    private Set<ArticleTagResponse> tags;
    private int commentsCount;
    private int likesCount;
}
