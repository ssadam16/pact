package com.technokratos.pact.article.repository;

import com.technokratos.pact.article.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByArticleId(UUID articleId, Pageable pageable);
    int countByArticleId(UUID articleId);
}
