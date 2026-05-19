package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.ArticleFilterRequest;
import com.technokratos.pact.article.dto.ArticleShortResponse;
import com.technokratos.pact.article.exception.ArticleNotFoundException;
import com.technokratos.pact.article.dto.ArticleCreateRequest;
import com.technokratos.pact.article.dto.ArticleResponse;
import com.technokratos.pact.article.mapper.ArticleMapper;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.model.ArticleLike;
import com.technokratos.pact.article.repository.ArticleLikeRepository;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import com.technokratos.pact.article.specification.ArticleSpecification;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
import com.technokratos.pact.security.model.UserDetailsImpl;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final AvatarService avatarService;
    private final ArticleLikeRepository articleLikeRepository;

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

    public ArticleResponse getArticle(UUID articleId, UUID currentUserId) {
        Article article = articleRepository.findById(articleId)
                        .orElseThrow(() -> ArticleNotFoundException.byId(articleId));

        ArticleResponse articleResponse = getFullArticleResponse(article, currentUserId);

        log.info("Returning Article (ID={})", articleId);
        log.debug("Article's author's avatarUrl={}", articleResponse.getAuthor().getAvatarUrl());

        return articleResponse;
    }

    @Transactional
    public ArticleResponse likeArticle(UUID articleId, UUID userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> ArticleNotFoundException.byId(articleId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        articleLikeRepository.save(ArticleLike.builder()
                .id(new ArticleLike.ArticleLikeId(user.getId(), article.getId()))
                .user(user)
                .article(article)
                .build());

        articleRepository.likeArticle(articleId);

        Article updatedArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> ArticleNotFoundException.byId(articleId));

        log.info("Article (ID={}) was liked by User (ID={})", articleId, userId);

        return getFullArticleResponse(updatedArticle, userId);
    }

    @Transactional
    public ArticleResponse unlikeArticle(UUID articleId, UUID userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> ArticleNotFoundException.byId(articleId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byId(userId));

        articleLikeRepository.delete(ArticleLike.builder()
                .id(new ArticleLike.ArticleLikeId(user.getId(), article.getId()))
                .user(user)
                .article(article)
                .build());

        articleRepository.unlikeArticle(articleId);

        Article updatedArticle = articleRepository.findById(articleId)
                .orElseThrow(() -> ArticleNotFoundException.byId(articleId));

        log.info("Article (ID={}) was unliked by User (ID={})", articleId, userId);

        return getFullArticleResponse(updatedArticle, userId);
    }

    public int getLikesCount(UUID articleId) {
        articleRepository.findById(articleId)
                .orElseThrow(() -> ArticleNotFoundException.byId(articleId));

        log.info("Returning likes count for article with ID={}", articleId);

        return articleLikeRepository.countByArticleId(articleId);
    }

    private ArticleResponse getFullArticleResponse(Article article) {
        ArticleResponse response = articleMapper.toArticleResponse(article);

        UUID currentUserId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            currentUserId = userDetails.getId();
        }

        response.setLiked(articleLikeRepository.existsByUserIdAndArticleId(currentUserId, article.getId()));
        response.getAuthor().setAvatarUrl(avatarService.getAvatarUrl(article.getAuthor().getAvatarFilename()));

        return response;
    }

    private ArticleResponse getFullArticleResponse(Article article, UUID currentUserId) {
        ArticleResponse response = articleMapper.toArticleResponse(article);

        response.setLiked(articleLikeRepository.existsByUserIdAndArticleId(currentUserId, article.getId()));
        response.getAuthor().setAvatarUrl(avatarService.getAvatarUrl(article.getAuthor().getAvatarFilename()));

        return response;
    }

    private ArticleShortResponse getFullArticleShortResponse(Article article) {
        ArticleShortResponse response = articleMapper.toArticleShortResponse(article);

        response.setCommentsCount(commentService.getCommentsCount(response.getId()));
        response.setLikesCount(getLikesCount(response.getId()));
        response.getAuthor().setAvatarUrl(avatarService.getAvatarUrl(article.getAuthor().getAvatarFilename()));

        return response;
    }

    public List<ArticleShortResponse> getArticlesByFilters(ArticleFilterRequest filterRequest) {
        log.info("Get article with filters: {}", filterRequest);

        Sort.Direction direction = Sort.Direction.fromString(filterRequest.sortType().toUpperCase());
        String sortField = filterRequest.sortBy();
        Sort sort = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(filterRequest.page(), filterRequest.size(), sort);
        Specification<Article> specification = ArticleSpecification.filterBy(
                filterRequest.tags(),
                filterRequest.games(),
                filterRequest.authorId(),
                filterRequest.search()
        );

        Page<Article> articlePage = articleRepository.findAll(specification, pageable);

        List<Article> sortedList;
        if (sortField.equals("logScore")) {
            sortedList = articlePage.getContent().stream()
                    .peek(a -> a.setLogScore(a.getLogScore() / getCountedTimeFormula(a)))
                    .sorted((a1, a2) -> Double.compare(a2.getLogScore(), a1.getLogScore()))
                    .toList();
        } else {
            sortedList = articlePage.getContent();
        }

        return sortedList.stream().map(this::getFullArticleShortResponse).collect(Collectors.toList());
    }

    private double getCountedTimeFormula(Article a) {
        return Math.pow(((double) getAgeInHours(a) / 5 + 2), 1.8);
    }

    public long getAgeInHours(Article article) {
        return Duration.between(article.getCreatedAt(), LocalDateTime.now()).toHours();
    }
}
