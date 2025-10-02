# YouTrack Integration with Discord (Java)

This Spring Boot app polls YouTrack notifications and sends them to a Discord channel via a webhook. It also lets you create YouTrack issues from Discord with a slash command. Although I have some prior exposure to Kotlin, I decided to use Java due to my familiarity with the language.

I chose to develop the application using Spring Boot for its quick setup, built-in scheduling (@Scheduled), and a clean code structure that also avoids much pre-configuration (besides API tokens and such) if you do not follow a framework.

## Features
- Uses YouTrack REST API with a personal token
- Sends key fields (Issue ID, Summary, Status/Priority/Comment, URL link) to Discord
- Configurable base URL, token, and Discord webhook
- Polls YouTrack on an interval (default 30 seconds), but the user can set this themself
- Create issues from Discord with `/youtrack` command in Dthe iscord channel message

## Requirements
- Java 17+ and Maven
- A YouTrack instance
- A Discord server you own

## Running the App
1. Set environment variables (can do this in an .env but in Spring Boot with IntelliJ it is easier to:
   Run → Edit Configurations → (edit) Environment Variables
```
YOUTRACK_URL=https://<your>.youtrack.cloud/api
YOUTRACK_TOKEN=perm:YOUR_TOKEN
YOUTRACK_PROJECT_SHORT=DEMO
DISCORD_WEBHOOK=https://discord.com/api/webhooks/...
DISCORD_BOT_TOKEN=YOUR_BOT_TOKEN   # only needed for /youtrack
POLL_SECONDS=30
```
2. Run:
```
mvn spring-boot:run
```

## Configuration
- **YouTrack**: Profile → Authentication → New token (select **YouTrack**); base URL must include `/api` (e.g., `https://team-intern.youtrack.cloud/api`); project short name like `DEMO`.
- **Discord Webhook**: Channel → Settings → Integrations → Webhooks → New Webhook → copy URL → set `DISCORD_WEBHOOK`.
- **Discord Bot** (for `/youtrack`):
  - Developer Portal → Applications → New Application → **Bot** → Reset Token → set `DISCORD_BOT_TOKEN`
  - Invite bot (replace `CLIENT_ID`):
    `https://discord.com/oauth2/authorize?client_id=CLIENT_ID&scope=bot%20applications.commands&permissions=19456`

### App properties (present in application.properties)
```
spring.application.name=Integration
youtrack.baseUrl=${YOUTRACK_URL}
youtrack.token=${YOUTRACK_TOKEN}
youtrack.projectShort=${YOUTRACK_PROJECT_SHORT:DEMO}
discord.webhookUrl=${DISCORD_WEBHOOK}
discord.botToken=${DISCORD_BOT_TOKEN}
poll.seconds=${POLL_SECONDS:30}
```

## Usage
- **Notifications**: make a change in YouTrack (assign, comment, change priority, @mention) → the app posts to your Discord webhook. Make sure to enable the appropriate settings to receive corresponding notifications.
- **Create issue**: in Discord (server with the bot), run `/youtrack summary: "Add new log in button"` → bot replies with the new issue key + link.

## Insights
- **Unauthorized/HTML on poll**: `YOUTRACK_URL` must include `/api` and `YOUTRACK_TOKEN` must be set.
- **Slash command missing**: invite with `applications.commands`; global commands can take ~60s on first create.
- **Only “admin” notifications**: the feed is per token/user; use a shared bot account/token for a team feed.
