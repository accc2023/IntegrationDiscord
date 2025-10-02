package com.arhan.integration;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {
    private final String token;
    private final SlashListener listener;

    public DiscordBotConfig(
            @Value("${discord.botToken}") String token,
            SlashListener listener) {
        this.token = token;
        this.listener = listener;
    }

    @PostConstruct
    public void start() throws Exception {
        var jda = JDABuilder.createDefault(token)
                .addEventListeners(listener)
                .setActivity(Activity.playing("/youtrack to create issue"))
                .build();
        jda.awaitReady();

        jda.updateCommands()
                .addCommands(net.dv8tion.jda.api.interactions.commands.build.Commands
                        .slash("youtrack", "Create a YouTrack issue from text")
                        .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
                                "summary", "Issue summary", true))
                .queue();
    }
}