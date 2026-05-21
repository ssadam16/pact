package com.technokratos.pact.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final OpenRouterClient openRouterClient;

    @PostMapping("/improve")
    public ResponseEntity<Map<String, String>> improveText(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text cannot be empty"));
        }

        String improved = openRouterClient.improveText(text);
        if (improved == null) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Processing error"));
        }

        return ResponseEntity.ok(Map.of("improvedText", improved));
    }
}