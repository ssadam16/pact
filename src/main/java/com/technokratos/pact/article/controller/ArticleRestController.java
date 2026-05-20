package com.technokratos.pact.article.controller;

import com.technokratos.pact.article.dto.ArticleFilterRequest;
import com.technokratos.pact.article.dto.ArticleResponse;
import com.technokratos.pact.article.dto.ArticleShortResponse;
import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.article.service.ArticleService;
import com.technokratos.pact.common.dto.ApiResponse;
import com.technokratos.pact.security.model.UserDetailsImpl;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleRestController {

    private final ArticleService articleService;

    @GetMapping
    public ResponseEntity<List<ArticleShortResponse>> findArticles(@RequestParam(required = false, defaultValue = "0") int page,
                                                   @RequestParam(required = false, defaultValue = "20") int size,
                                                   @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(required = false, defaultValue = "desc")
                                                       @Pattern(regexp = "asc|desc",
                                                               message = "sortType must be 'asc' or 'desc'") String sortType,
                                                   @RequestParam(required = false) String search,
                                                   @RequestParam(required = false) Set<ArticleTag.ArticleTagName> tags,
                                                   @RequestParam(required = false) Set<String> games,
                                                   @RequestParam(required = false) UUID authorId) {

        return ResponseEntity.ok(articleService.getArticlesByFilters(ArticleFilterRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortType(sortType)
                .search(search)
                .tags(tags)
                .games(games)
                .authorId(authorId)
                .build())
        );
    }

    @PostMapping("/{articleId}/like")
    public ResponseEntity<ArticleResponse> like(@PathVariable UUID articleId,
                                                @AuthenticationPrincipal UserDetailsImpl currentUser) {

        return ResponseEntity.ok(articleService.likeArticle(articleId, currentUser.getId()));
    }

    @PostMapping("/{articleId}/unlike")
    public ResponseEntity<ArticleResponse> unlike(@PathVariable UUID articleId,
                                                @AuthenticationPrincipal UserDetailsImpl currentUser) {

        return ResponseEntity.ok(articleService.unlikeArticle(articleId, currentUser.getId()));
    }
}
