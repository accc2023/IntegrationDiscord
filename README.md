# YouTrack Integration with Discord (Java Spring Boot)

This Spring Boot app polls YouTrack notifications and sends them to a Discord channel via a webhook. It also lets you create YouTrack issues from Discord with a slash command. Although I have some prior exposure to Kotlin, I decided to use Java due to my familiarity with the language.

I chose to develop the application using Spring Boot for its quick setup, built-in scheduling (@EnableScheduling), and a clean code structure that also avoids much pre-configuration (besides API tokens and such) if you do not follow a framework.

## Demo
- Video demonstration (3 minutes; no sound): https://drive.google.com/file/d/1FoWrKsSySpmsA8mabqGVct2j4U5E1b9R/view?usp=sharing
  - It goes through three use cases of using the YouTrack/Discord integration

## Features
- Uses YouTrack REST API with a personal token
- Sends key fields (Issue ID, Summary, Status/Priority/Comment, URL link) to Discord
- Configurable base URL, token, and Discord webhook
- Polls YouTrack on an interval (default 30 seconds), but the user can set this themself
- Create issues from Discord with `/youtrack` command in the Discord channel message

## Requirements
- Java 17+
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
YOUTRACK_PROJECT_ID=0-2
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
youtrack.projectId=${YOUTRACK_PROJECT_ID}
discord.webhookUrl=${DISCORD_WEBHOOK}
discord.botToken=${DISCORD_BOT_TOKEN}
poll.seconds=${POLL_SECONDS:30}
```

## Usage
- **Notifications**: make a change in YouTrack (assign, comment, change priority, @mention) → the app posts to your Discord webhook. Make sure to enable the appropriate settings to receive corresponding notifications.
- **Create issue**: in Discord (server with the bot), run `/youtrack summary: "Add new log in button"` → bot replies with the new issue text + link to the issue on YouTrack.

## Insights
- Overall, quite a fun, fast-paced project. It was also my first time utilizing the (Discord) webhook for a personal project, and it was quite easy to set up.
- I realised that sometimes the boilerplate code, with formatting especially, can be quite verbose and tricky to keep organised within the code base.
- I noticed that the timestamps correspond to GMT time. I only realized this after recording the demo, but the fix would be to import DateTimeFormatter and use its formatting options to set the time to your own timezone instead of using YouTrack's timing from the metadata.
