package com.technokratos.pact.article.service;

import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleTagDataLoader {

    private final ArticleTagRepository tagRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadDefaultTags() {
        boolean allExist = Arrays.stream(ArticleTag.ArticleTagName.values())
                .allMatch(tagRepository::existsByName);

        if (!allExist) {
            tagRepository.deleteAllInBatch();

            Arrays.stream(ArticleTag.ArticleTagName.values())
                    .map(ArticleTag::new)
                    .forEach(tagRepository::save);

            log.info("Article tags reinitialized");
        } else {
            log.info("Article tags already up to date");
        }
    }
}