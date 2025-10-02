package com.arhan.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Service
public class YouTrackService {
    private final WebClient youtrackClient;
    private static final ObjectMapper OM = new ObjectMapper();

    public YouTrackService(WebClient youtrackClient) { this.youtrackClient = youtrackClient; }

    // Uses Mono to structure received notifications from YouTrack API endpoint
    public Mono<List<JsonNode>> fetchNotifications() {
        String fields = "id,content,metadata,notified,read,updated";
        return youtrackClient.get()
                .uri("/users/notifications?fields=" + fields + "&$top=50")
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> {
                    try { return Arrays.asList(OM.readValue(body, JsonNode[].class)); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
    }

    // Decode the raw metadata output from base64
    public static String decodeB64Gzip(String b64) {
        if (b64 == null || b64.isBlank()) return "";
        try (GZIPInputStream gis = new GZIPInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(b64)))) {
            return new String(gis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    // From Discord bot API to create a POST request to YouTrack API endpoint (in JSON format)
    public Mono<JsonNode> createIssue(String summary, String projectShortOrId, boolean useShortName) {
        var om = new ObjectMapper();
        var body = om.createObjectNode();
        var proj = om.createObjectNode();
        if (useShortName) proj.put("shortName", projectShortOrId);
        else proj.put("id", projectShortOrId);
        body.set("project", proj);
        body.put("summary", summary);

        String fields = "idReadable,summary,project(shortName)";

        return youtrackClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/issues")
                        .queryParam("fields", fields)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> {
                    try { return om.readTree(s); }
                    catch (Exception e) { throw new RuntimeException("Parsing failed: " + e.getMessage()); }
                });
    }
}
