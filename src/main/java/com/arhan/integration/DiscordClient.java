package com.arhan.integration;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import org.springframework.http.MediaType;
import java.util.Map;

@Service
public class DiscordClient {
    private final WebClient http;
    private final String webhook;

    public DiscordClient(WebClient http, @Value("${discord.webhookUrl}") String webhook) {
        this.http = http; this.webhook = webhook;
    }

    public Mono<Void> send(String title, String markdown) {
        // Using mono since it is non-blocking and simplifies async calls
        Map<String,Object> embed = Map.of("title", title, "description", markdown);
        Map<String,Object> body  = Map.of("embeds", new Object[]{ embed },
                "allowed_mentions", Map.of("parse", new String[]{}));
        return http.post()
                .uri(webhook)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}
