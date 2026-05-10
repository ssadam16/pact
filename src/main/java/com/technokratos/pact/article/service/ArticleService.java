package com.technokratos.pact.article.service;

import com.technokratos.pact.article.exception.ArticleNotFoundException;
import com.technokratos.pact.article.dto.ArticleCreateRequest;
import com.technokratos.pact.article.dto.ArticleResponse;
import com.technokratos.pact.article.mapper.ArticleMapper;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final GameRepository gameRepository;
    private final ArticleMapper articleMapper;
    private final UserRepository userRepository;
    private final ArticleTagRepository tagRepository;
    private final CommentService commentService;

    @Transactional
    public UUID create(ArticleCreateRequest request, UUID authorId) {

        Article article = articleMapper.toArticle(request);

        String safeHtml = Jsoup.clean(request.content(), Safelist.relaxed());
        article.setContent(safeHtml);

        article.setAuthor(
                userRepository.findById(authorId)
                .orElseThrow(() -> UserNotFoundException.byId(authorId))
        );

        if (request.gameIds() != null && !request.gameIds().isEmpty()) {
            List<Game> games = gameRepository.findAllById(request.gameIds());
            article.setGames(new HashSet<>(games));
        }

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            article.setTags(new HashSet<>(tagRepository.findAllById(request.tagIds())));
        }

        Article saved = articleRepository.save(article);

        log.info("Article (ID={}) is created", saved.getId());

        return saved.getId();
    }

    public ArticleResponse getArticle(UUID articleId) {
        ArticleResponse articleResponse = articleMapper.toArticleResponse(
                articleRepository.findById(articleId)
                        .orElseThrow(() -> ArticleNotFoundException.byId(articleId))
        );

        articleResponse.setComments(commentService.getCommentsByArticleId(articleId));

        log.info("Returning Article (ID={})", articleId);

        return articleResponse;
    }
}
