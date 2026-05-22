package com.technokratos.pact.game.repository;

import com.technokratos.pact.game.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
    boolean existsByName(String name);
}
