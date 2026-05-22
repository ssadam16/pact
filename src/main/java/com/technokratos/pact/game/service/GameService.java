package com.technokratos.pact.game.service;

import com.technokratos.pact.game.dto.GameRequest;
import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.game.exception.GameAlreadyExistsException;
import com.technokratos.pact.game.exception.GameNotFoundException;
import com.technokratos.pact.game.mapper.GameMapper;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameMapper gameMapper;

    public List<GameResponse> findAll() {
        List<GameResponse> games = gameMapper.toGameResponseList(gameRepository.findAll());
        log.info("Returning games list (size={})", games.size());
        return games;
    }

    public GameResponse findById(UUID id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));
        log.info("Returning game with ID={}", id);
        return gameMapper.toGameResponse(game);
    }

    @Transactional
    public GameResponse create(GameRequest request) {
        if (gameRepository.existsByName(request.name())) {
            throw GameAlreadyExistsException.byName(request.name());
        }

        Game game = Game.builder()
                .name(request.name())
                .developer(request.developer())
                .steamLink(request.steamLink())
                .build();

        Game saved = gameRepository.save(game);
        log.info("Game created with ID={}, name={}", saved.getId(), saved.getName());
        return gameMapper.toGameResponse(saved);
    }

    @Transactional
    public GameResponse update(UUID id, GameRequest request) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));

        if (!game.getName().equals(request.name()) && gameRepository.existsByName(request.name())) {
            throw GameAlreadyExistsException.byName(request.name());
        }

        game.setName(request.name());
        game.setDeveloper(request.developer());
        game.setSteamLink(request.steamLink());

        Game updated = gameRepository.save(game);
        log.info("Game updated with ID={}", updated.getId());
        return gameMapper.toGameResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));
        gameRepository.delete(game);
        log.info("Game deleted with ID={}", id);
    }
}