package com.technokratos.pact.article.dto;

import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class ArticleResponse{
        private String title;
        private String content;
        private UserShortProfileResponse author;
        private Set<GameResponse> games;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Set<ArticleTagResponse> tags;
        private List<CommentResponse> comments;
}
