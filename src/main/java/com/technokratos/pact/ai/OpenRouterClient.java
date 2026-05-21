package com.technokratos.pact.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenRouterClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String openRouterUrl;

    public String improveText(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = getMapHttpEntity(text, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(openRouterUrl, entity, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());

            String improved = json.path("choices").get(0).path("message").path("content").asText();

            return improved.trim();
        } catch (Exception e) {
            log.error("OpenRouter internal error: {}", e.getMessage());
            return null;
        }
    }

    private static @NonNull HttpEntity<Map<String, Object>> getMapHttpEntity(String text, HttpHeaders headers) {
        String systemPrompt = """
                Ты — помощник на игровом форуме Pact. Твоя задача — улучшить текст статьи, который напишет пользователь.
                
                Правила:
                1. Сохрани все факты и смысл. Не добавляй и не удаляй информацию.
                2. Исправь орфографические, пунктуационные и грамматические ошибки.
                3. Сделай текст более читаемым и плавным: разбей на абзацы, улучши формулировки, убери повторы.
                4. НЕ используй Markdown, звездочки (*), решетки (#), дефисы для списков, подчеркивания и другие символы форматирования.
                5. НЕ добавляй заголовки, подзаголовки, эмодзи, смайлики и восклицательные знаки (кроме конца предложения).
                6. Только обычный текст с пробелами и переносами строк между абзацами.
                7. Не пиши слова-паразиты: "конечно", "вот", "давайте", "как вы просили", "рад помочь", "обращайтесь".
                8. Не объясняй, что ты сделал. Не добавляй фразы вроде "Вот улучшенный текст:" или "Я исправил ошибки:".
                9. Выдай ТОЛЬКО итоговый текст. Без приветствий, без прощаний, без комментариев.
                """;

        Map<String, Object> requestBody = Map.of(
                "model", "deepseek/deepseek-v4-flash:free",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", text)
                ),
                "temperature", 0.5,
                "max_tokens", 4000
        );

        return new HttpEntity<>(requestBody, headers);
    }
}