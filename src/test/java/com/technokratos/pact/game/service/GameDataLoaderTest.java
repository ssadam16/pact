package com.technokratos.pact.game.service;

import com.technokratos.pact.game.dto.SteamSpyGameDto;
import com.technokratos.pact.game.exception.CannotLoadSteamSpyGamesException;
import com.technokratos.pact.game.model.Game;
import com.technokratos.pact.game.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDataLoaderTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private GameDataLoader dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = new GameDataLoader(gameRepository, restClient);

        ReflectionTestUtils.setField(dataLoader, "gamesCount", 2);
        ReflectionTestUtils.setField(dataLoader, "steamSpyUrl", "http://steamspy");
        ReflectionTestUtils.setField(dataLoader, "steamBaseAppUrl", "http://store/app/");
    }

    @SuppressWarnings("unchecked")
    private void stubRestChain(Map<String, SteamSpyGameDto> body) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("http://steamspy")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(body);
    }

    private SteamSpyGameDto game(String name, String dev) {
        SteamSpyGameDto dto = new SteamSpyGameDto();
        dto.setName(name);
        dto.setDeveloper(dev);

        return dto;
    }

    @Test
    void loadSteamGames_savesLimitedSet() {
        Map<String, SteamSpyGameDto> body = new LinkedHashMap<>();

        body.put("570", game("Dota 2", "Valve"));
        body.put("730", game("Counter-Strike: Global Offensive", "Valve"));
        body.put("440", game("Team Fortress 2", "Valve"));

        stubRestChain(body);

        dataLoader.loadSteamGames();

        ArgumentCaptor<Collection<Game>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(gameRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void loadSteamGames_renamesCsgo() {
        Map<String, SteamSpyGameDto> body = new LinkedHashMap<>();

        body.put("730", game("Counter-Strike: Global Offensive", "Valve"));
        stubRestChain(body);

        dataLoader.loadSteamGames();

        ArgumentCaptor<Collection<Game>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(gameRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anyMatch(g -> g.getName().equals("Counter-Strike 2"));
    }

    @Test
    void loadSteamGames_emptyResponse_throws() {
        stubRestChain(Map.of());

        assertThatThrownBy(() -> dataLoader.loadSteamGames())
                .isInstanceOf(CannotLoadSteamSpyGamesException.class);
        verify(gameRepository, never()).saveAll(any());
    }

    @Test
    void init_whenWrongCount_reloads() {
        when(gameRepository.count()).thenReturn(10L);
        Map<String, SteamSpyGameDto> body = new LinkedHashMap<>();

        body.put("570", game("Dota 2", "Valve"));
        stubRestChain(body);

        dataLoader.init();

        verify(gameRepository).deleteAllInBatch();
        verify(gameRepository).saveAll(any());
    }

    @Test
    void init_whenExactly50_skips() {
        when(gameRepository.count()).thenReturn(50L);

        dataLoader.init();

        verify(gameRepository, never()).deleteAllInBatch();
        verify(gameRepository, never()).saveAll(any());
    }
}
