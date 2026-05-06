package com.technokratos.pact.game.service;

import com.technokratos.pact.game.dto.SteamSpyGameDto;
import com.technokratos.pact.game.exception.CannotLoadSteamSpyGamesException;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameDataLoader {

    private final GameRepository gameRepository;
    @Value("${steam.games.count}")
    private int gamesCount;

    @Value("${external-api.steam-spy.url}")
    private String steamSpyUrl;

    @Value("${external-api.steam.base-app-url}")
    private String steamBaseAppUrl;

    private final RestClient restClient;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        if (gameRepository.count() != 50) {
            gameRepository.deleteAllInBatch();
            loadSteamGames();
        } else {
            log.info("Game data already exists in DB");
        }
    }

    public void loadSteamGames() {
        Map<String, SteamSpyGameDto> topGames = restClient.get()
                .uri(steamSpyUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, SteamSpyGameDto>>() {});

        if (topGames == null || topGames.isEmpty()) {
            log.error("Cannot load games from Steam Spy");
            throw new CannotLoadSteamSpyGamesException("Failed to fetch top games from SteamSpy");
        }

        Set<Game> gamesToSave = topGames.entrySet().stream()
                .limit(gamesCount)
                .map(entry -> {
                    SteamSpyGameDto dto = entry.getValue();
                    String appId = entry.getKey();

                    return Game.builder()
                            .name(dto.getName().equals("Counter-Strike: Global Offensive") ?
                                    "Counter-Strike 2" :
                                    dto.getName())
                            .developer(dto.getDeveloper())
                            .steamLink("%s%s".formatted(steamBaseAppUrl, appId))
                            .build();
                })
                .collect(Collectors.toSet());

        gameRepository.saveAll(gamesToSave);

        log.info("Game data was initialized");

        log.debug("Saved {} games: {}", gamesToSave.size(),
                gamesToSave.stream().map(Game::getName).collect(Collectors.joining(", ")));
    }
}
