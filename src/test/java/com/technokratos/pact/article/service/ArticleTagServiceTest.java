package com.technokratos.pact.article.service;

import com.technokratos.pact.article.dto.ArticleTagResponse;
import com.technokratos.pact.article.mapper.ArticleTagMapper;
import com.technokratos.pact.article.model.ArticleTag;
import com.technokratos.pact.article.repository.ArticleTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleTagServiceTest {

    @Mock
    private ArticleTagRepository tagRepository;

    @Mock
    private ArticleTagMapper tagMapper;

    @InjectMocks
    private ArticleTagService articleTagService;

    @Test
    void findAll_mapsEveryTag() {
        ArticleTag guide = new ArticleTag(ArticleTag.ArticleTagName.GUIDE);
        ArticleTag review = new ArticleTag(ArticleTag.ArticleTagName.REVIEW);
        ArticleTagResponse guideDto = new ArticleTagResponse(UUID.randomUUID(), ArticleTag.ArticleTagName.GUIDE);
        ArticleTagResponse reviewDto = new ArticleTagResponse(UUID.randomUUID(), ArticleTag.ArticleTagName.REVIEW);

        when(tagRepository.findAll()).thenReturn(List.of(guide, review));
        when(tagMapper.toArticleTagResponse(guide)).thenReturn(guideDto);
        when(tagMapper.toArticleTagResponse(review)).thenReturn(reviewDto);

        List<ArticleTagResponse> result = articleTagService.findAll();

        assertThat(result).containsExactly(guideDto, reviewDto);
    }

    @Test
    void findAll_emptyRepo_returnsEmpty() {
        when(tagRepository.findAll()).thenReturn(List.of());

        assertThat(articleTagService.findAll()).isEmpty();
    }
}
