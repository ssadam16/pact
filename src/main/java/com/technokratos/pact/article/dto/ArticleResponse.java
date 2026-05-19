package com.technokratos.pact.article.dto;

import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class ArticleResponse{
        private UUID id;
        private String title;
        private String content;
        private UserShortProfileResponse author;
        private Set<GameResponse> games;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Set<ArticleTagResponse> tags;
        private int commentsCount;
        private int likesCount;
        private boolean isLiked;
}
