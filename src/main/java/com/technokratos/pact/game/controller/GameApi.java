package com.technokratos.pact.game.controller;

import com.technokratos.pact.game.dto.GameRequest;
import com.technokratos.pact.game.dto.GameResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Games", description = "Управление играми в каталоге")
@RequestMapping("/api/games")
public interface GameApi {

    @Operation(summary = "Получить все игры", description = "Возвращает список всех игр в каталоге")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список игр успешно получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content)
    })
    @GetMapping
    ResponseEntity<List<GameResponse>> getAllGames();

    @Operation(summary = "Получить игру по ID", description = "Возвращает игру по её уникальному идентификатору")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра найдена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Неверный формат ID", content = @Content)
    })
    @GetMapping("/{id}")
    ResponseEntity<GameResponse> getGameById(
            @Parameter(description = "UUID игры", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    );

    @Operation(summary = "Создать игру", description = "Добавляет новую игру в каталог")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Игра успешно создана",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверные данные запроса", content = @Content),
            @ApiResponse(responseCode = "409", description = "Игра с таким названием уже существует", content = @Content)
    })
    @PostMapping
    ResponseEntity<GameResponse> createGame(
            @Parameter(description = "Данные для создания игры", required = true)
            @Valid @RequestBody GameRequest request
    );

    @Operation(summary = "Обновить игру", description = "Обновляет существующую игру по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно обновлена",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameResponse.class))),
            @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Неверные данные запроса", content = @Content),
            @ApiResponse(responseCode = "409", description = "Игра с таким названием уже существует", content = @Content)
    })
    @PutMapping("/{id}")
    ResponseEntity<GameResponse> updateGame(
            @Parameter(description = "UUID игры", required = true) @PathVariable UUID id,
            @Parameter(description = "Данные для обновления", required = true) @Valid @RequestBody GameRequest request
    );

    @Operation(summary = "Удалить игру", description = "Удаляет игру из каталога по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Игра успешно удалена", content = @Content),
            @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Неверный формат ID", content = @Content)
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteGame(
            @Parameter(description = "UUID игры", required = true) @PathVariable UUID id
    );
}