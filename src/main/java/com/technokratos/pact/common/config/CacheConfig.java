package com.technokratos.pact.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ARTICLE = "article";
    public static final String MIN_ARTICLE = "minArticle";
    public static final String ARTICLES_BY_FILTERS = "articlesByFilters";
    public static final String LIKES_COUNT = "likesCount";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache article = buildCache(ARTICLE, Duration.ofMinutes(10), 1000);
        CaffeineCache minArticle = buildCache(MIN_ARTICLE, Duration.ofMinutes(30), 1000);
        CaffeineCache byFilters = buildCache(ARTICLES_BY_FILTERS, Duration.ofMinutes(2), 500);
        CaffeineCache likesCount = buildCache(LIKES_COUNT, Duration.ofMinutes(5), 2000);

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(article, minArticle, byFilters, likesCount));
        return manager;
    }

    private CaffeineCache buildCache(String name, Duration ttl, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .build());
    }
}
