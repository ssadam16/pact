package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.CommentCreateRequest;
import com.technokratos.pact.article.dto.CommentResponse;
import com.technokratos.pact.article.exception.ArticleNotFoundException;
import com.technokratos.pact.article.mapper.CommentMapper;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.model.Comment;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.CommentRepository;
import com.technokratos.pact.file.service.AvatarService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private CommentService commentService;

    private UUID userId;
    private UUID articleId;
    private User user;
    private Article article;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        articleId = UUID.randomUUID();
        user = User.builder().id(userId).username("said").avatarFilename("a.png").build();
        article = Article.builder().id(articleId).author(user).build();
    }

    private CommentResponse newResponse() {
        UserShortProfileResponse author = new UserShortProfileResponse();
        author.setId(userId);
        CommentResponse r = new CommentResponse();
        r.setAuthor(author);
        return r;
    }

    @Test
    void create_savesComment_andBumpsCounter() {
        CommentCreateRequest request = new CommentCreateRequest("nice", articleId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        Comment saved = Comment.builder().content("nice").author(user).article(article).build();
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
        when(commentMapper.toCommentResponse(saved)).thenReturn(newResponse());

        CommentResponse result = commentService.create(userId, request);

        assertThat(result).isNotNull();
        verify(articleRepository).addComment(articleId);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void create_userMissing_throws() {
        CommentCreateRequest request = new CommentCreateRequest("nice", articleId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(userId, request))
                .isInstanceOf(UserNotFoundException.class);
        verify(articleRepository, never()).addComment(any());
    }

    @Test
    void create_articleMissing_throws() {
        CommentCreateRequest request = new CommentCreateRequest("nice", articleId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(userId, request))
                .isInstanceOf(ArticleNotFoundException.class);
        verify(articleRepository, never()).addComment(any());
    }

    @Test
    void getCommentsByArticleId_mapsAndSetsAvatar() {
        Comment comment = Comment.builder().content("hi").author(user).article(article).build();
        Page<Comment> page = new PageImpl<>(List.of(comment));
        when(commentRepository.findByArticleId(eq(articleId), any(Pageable.class))).thenReturn(page);
        when(commentMapper.toCommentResponse(comment)).thenReturn(newResponse());
        when(avatarService.getAvatarUrl("a.png")).thenReturn("http://img/a.png");

        Page<CommentResponse> result = commentService.getCommentsByArticleId(articleId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthor().getAvatarUrl())
                .isEqualTo("http://img/a.png");
    }

    @Test
    void getCommentsCount_returnsCount() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(commentRepository.countByArticleId(articleId)).thenReturn(4);

        assertThat(commentService.getCommentsCount(articleId)).isEqualTo(4);
    }

    @Test
    void getCommentsCount_articleMissing_throws() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentsCount(articleId))
                .isInstanceOf(ArticleNotFoundException.class);
    }
}
