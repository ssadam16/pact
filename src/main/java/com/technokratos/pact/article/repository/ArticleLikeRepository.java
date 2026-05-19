package com.technokratos.pact.article.repository;

import com.technokratos.pact.article.model.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, ArticleLike.ArticleLikeId> {
    int countByArticleId(UUID articleId);
    boolean existsByUserIdAndArticleId(UUID userId, UUID articleId);
}
