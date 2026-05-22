package com.technokratos.pact.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenRouterClientTest {

    @Mock
    private RestTemplate restTemplate;

    private OpenRouterClient client;

    @BeforeEach
    void setUp() {
        client = new OpenRouterClient(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "openRouterUrl", "http://openrouter");
    }

    @Test
    void improveText_returnsTrimmedContent() {
        String body = "{\"choices\":[{\"message\":{\"content\":\"  улучшенный текст  \"}}]}";
        when(restTemplate.postForEntity(eq("http://openrouter"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        String result = client.improveText("исходник");

        assertThat(result).isEqualTo("улучшенный текст");
    }

    @Test
    void improveText_onError_returnsNull() {
        when(restTemplate.postForEntity(eq("http://openrouter"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("api down"));

        assertThat(client.improveText("исходник")).isNull();
    }

    @Test
    void improveText_malformedJson_returnsNull() {
        when(restTemplate.postForEntity(eq("http://openrouter"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not json"));

        assertThat(client.improveText("исходник")).isNull();
    }
}
