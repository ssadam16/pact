package com.technokratos.pact.article.dto;

import com.technokratos.pact.article.model.ArticleTag;

import java.util.UUID;

public record ArticleTagResponse (
        UUID id,
        ArticleTag.ArticleTagName name
) {
}
