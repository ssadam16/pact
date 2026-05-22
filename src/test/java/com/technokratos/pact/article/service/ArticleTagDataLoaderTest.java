package com.technokratos.pact.article.service;

import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleTagDataLoaderTest {

    @Mock
    private ArticleTagRepository tagRepository;

    @InjectMocks
    private ArticleTagDataLoader dataLoader;

    @Test
    void loadDefaultTags_whenSomeMissing_reinitializes() {
        when(tagRepository.existsByName(any())).thenReturn(false);

        dataLoader.loadDefaultTags();

        verify(tagRepository).deleteAllInBatch();
        verify(tagRepository, atLeastOnce()).save(any(ArticleTag.class));
    }

    @Test
    void loadDefaultTags_whenAllExist_doesNothing() {
        when(tagRepository.existsByName(any())).thenReturn(true);

        dataLoader.loadDefaultTags();

        verify(tagRepository, never()).deleteAllInBatch();
        verify(tagRepository, never()).save(any());
    }
}
