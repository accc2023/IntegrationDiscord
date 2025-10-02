package com.arhan.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class Config {
    @Bean
    WebClient http() { return WebClient.builder().build(); }

    @Bean WebClient youtrackClient(
            @Value("${youtrack.baseUrl}") String base,
            @Value("${youtrack.token}") String token) {
        return WebClient.builder()
                .baseUrl(base)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
