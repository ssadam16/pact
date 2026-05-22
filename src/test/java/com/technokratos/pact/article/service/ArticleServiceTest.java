package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.ArticleCreateRequest;
import com.technokratos.pact.article.dto.ArticleFilterRequest;
import com.technokratos.pact.article.dto.ArticleResponse;
import com.technokratos.pact.article.dto.ArticleShortResponse;
import com.technokratos.pact.article.exception.ArticleNotFoundException;
import com.technokratos.pact.article.mapper.ArticleMapper;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.model.ArticleLike;
import com.technokratos.pact.article.repository.ArticleLikeRepository;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleTagRepository tagRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private AvatarService avatarService;

    @Mock
    private ArticleLikeRepository articleLikeRepository;

    @InjectMocks
    private ArticleService articleService;

    private UUID articleId;
    private UUID userId;
    private Article article;
    private User author;

    @BeforeEach
    void setUp() {
        articleId = UUID.randomUUID();
        userId = UUID.randomUUID();

        author = User.builder()
                .id(userId)
                .username("said")
                .avatarFilename("avatar.png")
                .build();

        article = Article.builder()
                .id(articleId)
                .title("Some long title here")
                .content("content")
                .author(author)
                .createdAt(LocalDateTime.now().minusHours(3))
                .build();
    }

    private ArticleResponse responseWithAuthor() {
        UserShortProfileResponse a = new UserShortProfileResponse();
        a.setId(userId);
        a.setUsername("said");
        return ArticleResponse.builder().id(articleId).author(a).build();
    }

    private ArticleShortResponse shortWithAuthor() {
        UserShortProfileResponse a = new UserShortProfileResponse();
        a.setId(userId);
        ArticleShortResponse r = new ArticleShortResponse();
        r.setId(articleId);
        r.setAuthor(a);
        return r;
    }

    @Test
    void create_savesArticle_withGamesAndTags() {
        UUID gameId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        ArticleCreateRequest request = new ArticleCreateRequest(
                "Title that is long enough",
                "<p>hello</p><script>alert(1)</script>",
                List.of(gameId),
                List.of(tagId));

        Article mapped = Article.builder().title("Title that is long enough").build();
        when(articleMapper.toArticle(request)).thenReturn(mapped);
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(gameRepository.findAllById(List.of(gameId))).thenReturn(List.of(new Game()));
        when(tagRepository.findAllById(List.of(tagId))).thenReturn(List.of());
        when(articleRepository.save(mapped)).thenAnswer(inv -> {
            Article a = inv.getArgument(0);
            a.setId(articleId);
            return a;
        });

        UUID result = articleService.create(request, userId);

        assertThat(result).isEqualTo(articleId);
        assertThat(mapped.getContent()).doesNotContain("script");
        assertThat(mapped.getAuthor()).isEqualTo(author);
        verify(gameRepository).findAllById(List.of(gameId));
    }

    @Test
    void create_withoutGamesAndTags_skipsLookups() {
        ArticleCreateRequest request = new ArticleCreateRequest(
                "Title that is long enough", "<p>text</p>", List.of(), null);

        Article mapped = Article.builder().build();
        when(articleMapper.toArticle(request)).thenReturn(mapped);
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(articleRepository.save(mapped)).thenReturn(article);

        articleService.create(request, userId);

        verify(gameRepository, never()).findAllById(anyList());
        verify(tagRepository, never()).findAllById(anyList());
    }

    @Test
    void create_authorMissing_throws() {
        ArticleCreateRequest request = new ArticleCreateRequest(
                "Title that is long enough", "<p>text</p>", null, null);
        when(articleMapper.toArticle(request)).thenReturn(Article.builder().build());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.create(request, userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getMinArticle_returnsResponse_withAvatar() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleMapper.toArticleResponse(article)).thenReturn(responseWithAuthor());
        when(avatarService.getAvatarUrl("avatar.png")).thenReturn("http://img/avatar.png");

        ArticleResponse result = articleService.getMinArticle(articleId);

        assertThat(result.getAuthor().getAvatarUrl()).isEqualTo("http://img/avatar.png");
    }

    @Test
    void getMinArticle_missing_throws() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getMinArticle(articleId))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    void getArticle_returnsFullResponse() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleMapper.toArticleResponse(article)).thenReturn(responseWithAuthor());
        when(articleLikeRepository.existsByUserIdAndArticleId(userId, articleId)).thenReturn(true);
        when(avatarService.getAvatarUrl("avatar.png")).thenReturn("http://img/a.png");

        ArticleResponse result = articleService.getArticle(articleId, userId);

        assertThat(result.isLiked()).isTrue();
        assertThat(result.getAuthor().getAvatarUrl()).isEqualTo("http://img/a.png");
    }

    @Test
    void getArticle_missing_throws() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getArticle(articleId, userId))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    void likeArticle_savesLike_andReturnsUpdated() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(articleMapper.toArticleResponse(article)).thenReturn(responseWithAuthor());
        when(articleLikeRepository.existsByUserIdAndArticleId(userId, articleId)).thenReturn(true);
        when(avatarService.getAvatarUrl(any())).thenReturn("url");

        ArticleResponse result = articleService.likeArticle(articleId, userId);

        verify(articleLikeRepository).save(any(ArticleLike.class));
        verify(articleRepository).likeArticle(articleId);
        assertThat(result.isLiked()).isTrue();
    }

    @Test
    void likeArticle_userMissing_throws() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.likeArticle(articleId, userId))
                .isInstanceOf(UserNotFoundException.class);
        verify(articleRepository, never()).likeArticle(any());
    }

    @Test
    void unlikeArticle_deletesLike_andReturnsUpdated() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));
        when(articleMapper.toArticleResponse(article)).thenReturn(responseWithAuthor());
        when(articleLikeRepository.existsByUserIdAndArticleId(userId, articleId)).thenReturn(false);
        when(avatarService.getAvatarUrl(any())).thenReturn("url");

        ArticleResponse result = articleService.unlikeArticle(articleId, userId);

        verify(articleLikeRepository).delete(any(ArticleLike.class));
        verify(articleRepository).unlikeArticle(articleId);
        assertThat(result.isLiked()).isFalse();
    }

    @Test
    void unlikeArticle_articleMissing_throws() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.unlikeArticle(articleId, userId))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    void getLikesCount_returnsCount() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleLikeRepository.countByArticleId(articleId)).thenReturn(7);

        assertThat(articleService.getLikesCount(articleId)).isEqualTo(7);
    }

    @Test
    void getLikesCount_missing_throws() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> articleService.getLikesCount(articleId))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    void getArticlesByFilters_default_returnsMappedShortResponses() {
        ArticleFilterRequest filter = ArticleFilterRequest.builder()
                .page(0).size(10).sortBy("createdAt").sortType("desc")
                .tags(Set.of()).games(Set.of()).build();

        Page<Article> page = new PageImpl<>(List.of(article));
        when(articleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(articleMapper.toArticleShortResponse(article)).thenReturn(shortWithAuthor());
        when(commentService.getCommentsCount(articleId)).thenReturn(2);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleLikeRepository.countByArticleId(articleId)).thenReturn(5);
        when(avatarService.getAvatarUrl("avatar.png")).thenReturn("url");

        List<ArticleShortResponse> result = articleService.getArticlesByFilters(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommentsCount()).isEqualTo(2);
        assertThat(result.get(0).getLikesCount()).isEqualTo(5);
    }

    @Test
    void getArticlesByFilters_byLogScore_sortsDescending() {
        ArticleFilterRequest filter = ArticleFilterRequest.builder()
                .page(0).size(10).sortBy("logScore").sortType("desc")
                .tags(Set.of()).games(Set.of()).build();

        Article low = Article.builder()
                .id(UUID.randomUUID()).author(author).logScore(10.0)
                .createdAt(LocalDateTime.now().minusHours(1)).build();
        Article high = Article.builder()
                .id(UUID.randomUUID()).author(author).logScore(1000.0)
                .createdAt(LocalDateTime.now().minusHours(1)).build();

        Page<Article> page = new PageImpl<>(List.of(low, high));
        when(articleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(articleMapper.toArticleShortResponse(any(Article.class))).thenAnswer(inv -> {
            Article a = inv.getArgument(0);
            ArticleShortResponse r = shortWithAuthor();
            r.setId(a.getId());
            return r;
        });
        when(commentService.getCommentsCount(any())).thenReturn(0);
        when(articleRepository.findById(any())).thenAnswer(inv ->
                Optional.of(Article.builder().id(inv.getArgument(0)).author(author).build()));
        when(articleLikeRepository.countByArticleId(any())).thenReturn(0);
        when(avatarService.getAvatarUrl(any())).thenReturn("url");

        List<ArticleShortResponse> result = articleService.getArticlesByFilters(filter);

        assertThat(result.get(0).getId()).isEqualTo(high.getId());
    }

    @Test
    void getAgeInHours_computesFromCreatedAt() {
        Article a = Article.builder()
                .createdAt(LocalDateTime.now().minusHours(5)).build();

        assertThat(articleService.getAgeInHours(a)).isBetween(4L, 5L);
    }
}
