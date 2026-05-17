package com.technokratos.pact.article.dto;

import com.technokratos.pact.article.model.ArticleTag;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Builder
public record ArticleFilterRequest(
        int page,
        int size,
        String sortBy,
        String sortType,
        String search,
        Set<ArticleTag.ArticleTagName> tags,
        Set<String> games,
        UUID authorId
) {
}
