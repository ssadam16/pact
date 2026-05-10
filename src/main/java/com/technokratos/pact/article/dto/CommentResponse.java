package com.technokratos.pact.article.dto;

import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private String content;
    private UserShortProfileResponse author;
    private ArticleResponse article;
    private LocalDateTime createdAt;
}
