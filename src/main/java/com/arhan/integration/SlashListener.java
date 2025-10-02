package com.arhan.integration;

import com.fasterxml.jackson.databind.JsonNode;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SlashListener extends ListenerAdapter {

    private final YouTrackService youtrack;
    private final String youtrackApiBase;
    private final String projectId;

    public SlashListener(YouTrackService youtrack,
                         @Value("${youtrack.baseUrl}") String youtrackApiBase,
                         @Value("${youtrack.projectId}") String projectId) {
        this.youtrack = youtrack;
        this.youtrackApiBase = youtrackApiBase;
        this.projectId = projectId; // "DEMO" annotation of issue
    }

    // We will use JSON node to structure our incoming text
    // Includes try/catch error handling for empty summaries
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("youtrack")) return;

        var opt = event.getOption("summary");
        if (opt == null || opt.getAsString().isBlank()) {
            event.reply("Please provide a non-empty summary.").setEphemeral(true).queue();
            return;
        }

        String summary = opt.getAsString();
        event.deferReply(true).queue();

        Mono<JsonNode> created = youtrack.createIssue(summary, projectId);

        created.subscribe(
                node -> {
                    String idReadable = node.path("idReadable").asText("NEW");
                    String youtrackWebBase = youtrackApiBase.replaceAll("/api/?$", "");
                    String url = (youtrackWebBase.endsWith("/") ? youtrackWebBase : youtrackWebBase + "/") + "issue/" + idReadable;
                    event.getHook().sendMessage("**Created:** " + idReadable + "\n[" + summary + "](" + url + ")")
                            .setEphemeral(false).queue();
                },
                err -> event.getHook().sendMessage("Failed to create issue: " + err.getMessage())
                        .setEphemeral(true).queue()
        );
    }
}