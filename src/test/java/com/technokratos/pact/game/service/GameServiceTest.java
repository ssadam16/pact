package com.technokratos.pact.game.service;

import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.game.mapper.GameMapper;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
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
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameMapper gameMapper;

    @InjectMocks
    private GameService gameService;

    @Test
    void findAll_returnsMappedGames() {
        Game game = Game.builder().id(UUID.randomUUID()).name("Dota 2").build();
        GameResponse dto = new GameResponse(game.getId(), "Dota 2");

        when(gameRepository.findAll()).thenReturn(List.of(game));
        when(gameMapper.toGameResponseList(List.of(game))).thenReturn(List.of(dto));

        List<GameResponse> result = gameService.findAll();

        assertThat(result).containsExactly(dto);
    }

    @Test
    void findAll_empty_returnsEmpty() {
        when(gameRepository.findAll()).thenReturn(List.of());
        when(gameMapper.toGameResponseList(List.of())).thenReturn(List.of());

        assertThat(gameService.findAll()).isEmpty();
    }
}
