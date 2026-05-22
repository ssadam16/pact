package com.technokratos.pact.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameRequest(
        @NotBlank(message = "Название игры не может быть пустым")
        @Size(min = 1, max = 100, message = "Название игры должно быть от 1 до 100 символов")
        String name,

        @NotBlank(message = "Разработчик не может быть пустым")
        @Size(min = 1, max = 100, message = "Название разработчика должно быть от 1 до 100 символов")
        String developer,

        @NotBlank(message = "Ссылка на Steam не может быть пустой")
        @Size(max = 500, message = "Ссылка не может превышать 500 символов")
        String steamLink
) {}