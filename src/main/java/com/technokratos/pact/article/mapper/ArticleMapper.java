package com.technokratos.pact.article.mapper;

import com.technokratos.pact.article.dto.ArticleCreateRequest;
import com.technokratos.pact.article.dto.ArticleResponse;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.game.mapper.GameMapper;
import com.technokratos.pact.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class, GameMapper.class, ArticleTagMapper.class})
public interface ArticleMapper {

    @Mapping(target = "content", ignore = true)
    Article toArticle(ArticleCreateRequest request);

    ArticleResponse toArticleResponse(Article article);
}
