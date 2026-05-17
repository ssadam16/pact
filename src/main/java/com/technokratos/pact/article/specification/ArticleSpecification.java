package com.technokratos.pact.article.specification;

import com.technokratos.pact.article.model.Article;
import com.technokratos.pact.article.model.ArticleTag;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ArticleSpecification {

    public static Specification<Article> filterBy(Set<ArticleTag.ArticleTagName> tags,
                                                  Set<String> games,
                                                  UUID authorId,
                                                  String searchText) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tags != null && !tags.isEmpty()) {
                predicates.add(root.join("tags").get("name").in(tags));
            }

            if (games != null && !games.isEmpty()) {
                predicates.add(root.join("games").get("name").in(games));
            }

            if (authorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("author").get("id"), authorId));
            }

            if (searchText != null && !searchText.isBlank()) {
                String pattern = "%" + searchText.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate contentMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), pattern);
                predicates.add(criteriaBuilder.or(titleMatch, contentMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}