package com.technokratos.pact.article.repository;

import com.technokratos.pact.article.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Page<Article> findAll(Specification<Article> spec, Pageable pageable);
}
