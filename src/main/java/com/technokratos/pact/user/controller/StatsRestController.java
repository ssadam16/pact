package com.technokratos.pact.user.controller;

import com.technokratos.pact.user.dto.ProfileStatsResponse;
import com.technokratos.pact.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stats")
public class StatsRestController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ProfileStatsResponse> getStats(UUID userId) {
        return ResponseEntity.ok(userService.getStats(userId));
    }
}
