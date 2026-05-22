package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.ArticleFilterRequest;
import com.technokratos.pact.article.dto.ArticleResponse;
import com.technokratos.pact.article.mapper.ArticleMapper;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.repository.ArticleLikeRepository;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import com.technokratos.pact.common.config.CacheConfig;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.game.repository.GameRepository;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig({CacheConfig.class, ArticleServiceCacheTest.Config.class})
class ArticleServiceCacheTest {

    static class Config {
        @Bean
        ArticleRepository articleRepository() {
            return Mockito.mock(ArticleRepository.class);
        }

        @Bean
        GameRepository gameRepository() {
            return Mockito.mock(GameRepository.class);
        }

        @Bean
        ArticleMapper articleMapper() {
            return Mockito.mock(ArticleMapper.class);
        }

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        ArticleTagRepository tagRepository() {
            return Mockito.mock(ArticleTagRepository.class);
        }

        @Bean
        CommentService commentService() {
            return Mockito.mock(CommentService.class);
        }

        @Bean
        AvatarService avatarService() {
            return Mockito.mock(AvatarService.class);
        }

        @Bean
        ArticleLikeRepository articleLikeRepository() {
            return Mockito.mock(ArticleLikeRepository.class);
        }

        @Bean
        ArticleService articleService(ArticleRepository articleRepository,
                                      GameRepository gameRepository,
                                      ArticleMapper articleMapper,
                                      UserRepository userRepository,
                                      ArticleTagRepository tagRepository,
                                      CommentService commentService,
                                      AvatarService avatarService,
                                      ArticleLikeRepository articleLikeRepository) {
            return new ArticleService(articleRepository, gameRepository, articleMapper,
                    userRepository, tagRepository, commentService, avatarService, articleLikeRepository);
        }
    }

    @Autowired
    private ArticleService articleService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AvatarService avatarService;
    @Autowired
    private ArticleLikeRepository articleLikeRepository;

    private UUID articleId;
    private UUID userId;
    private Article article;

    @BeforeEach
    void setUp() {
        articleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        User author = User.builder().id(userId).username("said").avatarFilename("a.png").build();
        article = Article.builder()
                .id(articleId).title("Title goes here").author(author)
                .createdAt(LocalDateTime.now().minusHours(1)).build();

        Mockito.reset(articleRepository, articleMapper, userRepository, avatarService, articleLikeRepository);
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    private ArticleResponse response() {
        UserShortProfileResponse a = new UserShortProfileResponse();
        a.setId(userId);
        return ArticleResponse.builder().id(articleId).author(a).build();
    }

    @Test
    void getMinArticle_isCached_afterFirstCall() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleMapper.toArticleResponse(article)).thenReturn(response());
        when(avatarService.getAvatarUrl(any())).thenReturn("url");

        articleService.getMinArticle(articleId);
        articleService.getMinArticle(articleId);

        verify(articleRepository, times(1)).findById(articleId);
        assertThat(cacheManager.getCache(CacheConfig.MIN_ARTICLE).get(articleId)).isNotNull();
    }

    @Test
    void getArticle_isCached_perUser() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleMapper.toArticleResponse(article)).thenReturn(response());
        when(articleLikeRepository.existsByUserIdAndArticleId(any(), any())).thenReturn(false);
        when(avatarService.getAvatarUrl(any())).thenReturn("url");

        articleService.getArticle(articleId, userId);
        articleService.getArticle(articleId, userId);

        verify(articleRepository, times(1)).findById(articleId);
        assertThat(cacheManager.getCache(CacheConfig.ARTICLE).get(articleId + "_" + userId)).isNotNull();
    }

    @Test
    void getArticlesByFilters_isCached() {
        ArticleFilterRequest filter = ArticleFilterRequest.builder()
                .page(0).size(10).sortBy("createdAt").sortType("desc")
                .tags(Set.of()).games(Set.of()).build();

        Page<Article> page = new PageImpl<>(List.of());
        when(articleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        articleService.getArticlesByFilters(filter);
        articleService.getArticlesByFilters(filter);

        verify(articleRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getLikesCount_isCached() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleLikeRepository.countByArticleId(articleId)).thenReturn(9);

        articleService.getLikesCount(articleId);
        articleService.getLikesCount(articleId);

        verify(articleLikeRepository, times(1)).countByArticleId(articleId);
        assertThat(cacheManager.getCache(CacheConfig.LIKES_COUNT).get(articleId)).isNotNull();
    }

    @Test
    void likeArticle_evictsCaches() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleMapper.toArticleResponse(article)).thenReturn(response());
        when(userRepository.findById(userId)).thenReturn(Optional.of(article.getAuthor()));
        when(articleLikeRepository.existsByUserIdAndArticleId(any(), any())).thenReturn(true);
        when(avatarService.getAvatarUrl(any())).thenReturn("url");

        articleService.getMinArticle(articleId);
        assertThat(cacheManager.getCache(CacheConfig.MIN_ARTICLE).get(articleId)).isNotNull();

        articleService.likeArticle(articleId, userId);

        assertThat(cacheManager.getCache(CacheConfig.MIN_ARTICLE).get(articleId)).isNull();
    }
}
