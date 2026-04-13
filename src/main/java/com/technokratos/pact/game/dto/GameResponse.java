package com.technokratos.pact.game.dto;

import java.util.UUID;

public record GameResponse (
        UUID id,
        String name
) {
}
