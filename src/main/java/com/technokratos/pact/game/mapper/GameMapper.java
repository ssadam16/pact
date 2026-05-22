package com.technokratos.pact.game.mapper;

import com.technokratos.pact.game.dto.GameResponse;
import com.technokratos.pact.game.model.Game;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GameMapper {
    GameResponse toGameResponse(Game game);
    List<GameResponse> toGameResponseList(List<Game> games);
}