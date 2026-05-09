package com.technokratos.pact.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ArticleCreateRequest(

        @NotBlank(message = "Заголовок статьи не может быть пустым")
        @Size(min = 10, message = "Заголовок статьи не может быть меньше 10 символов")
        String title,

        @NotBlank(message = "Содержимое статьи не можем быть пустым")
        @Size(min = 300, message = "Содержимое статьи не может быть меньше 300 символов")
        String content,

        @Size(max = 3, message = "Статья не может быть связана более чем с 3 играми")
        List<UUID> gameIds,

        @Size(max = 5, message = "Статья не может быть связана более чем с 5 тегами")
        List<UUID> tagIds
) {
}