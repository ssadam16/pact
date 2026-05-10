package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.CommentCreateRequest;
import com.technokratos.pact.article.dto.CommentResponse;
import com.technokratos.pact.article.exception.ArticleNotFoundException;
import com.technokratos.pact.article.mapper.CommentMapper;
import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.model.Comment;
import com.technokratos.pact.article.repository.ArticleRepository;
import com.technokratos.pact.article.repository.CommentRepository;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final ArticleRepository articleRepository;

    public CommentResponse create(UUID authorId, CommentCreateRequest request) {
        User user = userRepository.findById(authorId)
                .orElseThrow(() -> UserNotFoundException.byId(authorId));

        Article article = articleRepository.findById(request.articleId())
                .orElseThrow(() -> ArticleNotFoundException.byId(request.articleId()));

        Comment comment = Comment.builder()
                .author(user)
                .content(request.content())
                .article(article)
                .build();

        log.info("Comment was created (userId={}, articleId={})", user.getId(), article.getId());

        return commentMapper.toCommentResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getCommentsByArticleId(UUID articleId) {
        log.info("Returning comments for article with ID={}", articleId);
        return commentMapper.toCommentResponseList(commentRepository.findAllByArticleId(articleId));
    }
}
