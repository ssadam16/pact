package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.ArticleTagResponse;
import com.technokratos.pact.article.mapper.ArticleTagMapper;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleTagService {

    private final ArticleTagRepository tagRepository;
    private final ArticleTagMapper tagMapper;

    public List<ArticleTagResponse> findAll() {
        return tagRepository.findAll().stream()
                .map(tagMapper::toArticleTagResponse)
                .toList();
    }
}