package com.technokratos.pact.article.repository;

import com.technokratos.pact.article.model.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, UUID> {
}
