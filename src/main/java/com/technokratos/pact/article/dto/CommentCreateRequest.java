package com.technokratos.pact.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CommentCreateRequest (

     @NotBlank(message = "Содержимое комментария не может быть пустым")
     @Size(min = 1, max = 500, message = "Комментарий должен быть от 1 до 500 символов")
     String content,

     @NotNull(message = "ID поста обязателен")
     UUID articleId
) {
}
