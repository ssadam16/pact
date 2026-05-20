package com.technokratos.pact.article.repository;

import com.technokratos.pact.article.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Page<Article> findAll(Specification<Article> spec, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE article SET " +
            "likes_count = likes_count + 1, " +
            "log_score = LOG10(1 + (likes_count + 1) + comments_count) " +
            "WHERE id = :id",
            nativeQuery = true)
    void likeArticle(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE article SET " +
            "likes_count = likes_count - 1, " +
            "log_score = LOG10(1 + (likes_count - 1) + comments_count) " +
            "WHERE id = :id AND likes_count > 0",
            nativeQuery = true)
    void unlikeArticle(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE article SET " +
            "comments_count = comments_count + 1, " +
            "log_score = LOG10(1 + likes_count + (comments_count + 1)) " +
            "WHERE id = :id",
            nativeQuery = true)
    void addComment(@Param("id") UUID id);

    int countByAuthorId(UUID authorId);
}
