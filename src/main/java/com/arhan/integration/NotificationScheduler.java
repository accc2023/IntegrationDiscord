package com.arhan.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.arhan.integration.YouTrackService.decodeB64Gzip;

@Component
public class NotificationScheduler {
    private final YouTrackService youtrack;
    private final DiscordClient discord;
    private final String apiURL;
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final Path statePath = Paths.get(System.getProperty("user.home"), ".yt_seen_ids.txt");

    public NotificationScheduler(YouTrackService youtrack, DiscordClient discord,
                                 @Value("${youtrack.baseUrl}") String apiURL) {
        this.youtrack = youtrack;
        this.discord = discord;
        this.apiURL = apiURL.replaceAll("/api/?$", ""); // regex to eliminate trailing /api
        loadSeen(); // Load the texts that have been seen
    }

    // Two below functions load and save all notification IDs to avoid the webhook reposting
    // the same messages upon restart
    private void loadSeen() {
        try { if (Files.exists(statePath)) Files.readAllLines(statePath).forEach(seen::add); }
        catch (Exception ignore) {}
    }

    private void saveSeen() {
        try { Files.write(statePath, seen, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING); }
        catch (Exception ignore) {}
    }

    // Keeping poll time a secret (and since time is measured in ms we multiply by 1000)
    // Uses EnableScheduling interface to fetch notifications at specified time interval defined by user
    @Scheduled(fixedDelayString = "#{${poll.seconds} * 1000}")
    public void tick() {
        youtrack.fetchNotifications()
                .publishOn(Schedulers.boundedElastic())
                .subscribe(items -> {
                    items.sort(Comparator.comparingLong(n -> n.path("updated").asLong(n.path("notified").asLong(0))));
                    for (JsonNode n : items) {
                        String id = n.path("id").asText();
                        if (id == null || id.isBlank() || seen.contains(id)) continue;

                        String metaJson = decodeB64Gzip(n.path("metadata").asText(""));
                        String md;
                        try { md = YtFormatter.fromMetadata(metaJson, apiURL); }
                        catch (Exception ex) { md = decodeB64Gzip(n.path("content").asText("")); }

                        discord.send("YouTrack notification", md).block();
                        seen.add(id);
                    }
                    if (!items.isEmpty()) saveSeen();
                }, err -> System.err.println("Error polling data from seen: " + err.getMessage()));
    }
}
