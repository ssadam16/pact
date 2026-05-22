package com.technokratos.pact.game.controller;

import com.technokratos.pact.game.dto.GameRequest;
import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GameRestController implements GameApi {

    private final GameService gameService;

    @Override
    public ResponseEntity<List<GameResponse>> getAllGames() {
        return ResponseEntity.ok(gameService.findAll());
    }

    @Override
    public ResponseEntity<GameResponse> getGameById(UUID id) {
        return ResponseEntity.ok(gameService.findById(id));
    }

    @Override
    public ResponseEntity<GameResponse> createGame(GameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.create(request));
    }

    @Override
    public ResponseEntity<GameResponse> updateGame(UUID id, GameRequest request) {
        return ResponseEntity.ok(gameService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> deleteGame(UUID id) {
        gameService.delete(id);
        return ResponseEntity.noContent().build();
    }
}