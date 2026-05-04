package com.technokratos.pact.article.dto;

import com.technokratos.pact.article.model.ArticleTag;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record ArticleCreateRequest(

        @NotBlank
        String title,

        @NotBlank
        String content,

        List<UUID> gameIds,

        List<UUID> tagIds
) {
}