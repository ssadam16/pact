package com.technokratos.pact.article.mapper;

import com.technokratos.pact.article.dto.ArticleTagResponse;
import com.technokratos.pact.article.model.ArticleTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel =  "spring")
public interface ArticleTagMapper {

    @Mapping(target = "articles", ignore = true)
    ArticleTag toArticleTag(ArticleTagResponse tag);
    ArticleTagResponse toArticleTagResponse(ArticleTag tag);
}
