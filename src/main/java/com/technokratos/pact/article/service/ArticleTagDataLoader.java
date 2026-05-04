package com.technokratos.pact.article.service;

import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleTagDataLoader {

    private final ArticleTagRepository tagRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadDefaultTags() {
        Set<ArticleTag> defaultTags = Arrays.stream(ArticleTag.ArticleTagName.values())
                .map(ArticleTag::new)
                .collect(Collectors.toSet());

        tagRepository.deleteAll();
        tagRepository.saveAll(defaultTags);

        log.info("Article tags are initialized");
    }
}
