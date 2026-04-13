package com.technokratos.pact.game.service;

import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.game.mapper.GameMapper;
import com.technokratos.pact.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
