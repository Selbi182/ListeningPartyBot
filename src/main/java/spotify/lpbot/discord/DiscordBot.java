package spotify.lpbot.discord;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import spotify.api.events.SpotifyApiLoggedInEvent;

@EnableScheduling
@Component
public class DiscordBot {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordBot.class);

  private final DiscordMessageHandler discordMessageHandler;

  private DiscordApi api;

  @Value("${discord.api_token}")
  private String discordApiToken;

  DiscordBot(DiscordMessageHandler discordMessageHandler) {
    this.discordMessageHandler = discordMessageHandler;
  }

  @EventListener(SpotifyApiLoggedInEvent.class)
  public void login() {
    LOGGER.info("Successfully logged into Spotify!");
    try {
      LOGGER.info("Connecting ListeningPartyBot to Discord...");

      // Login
      this.api = new DiscordApiBuilder()
        .setToken(discordApiToken)
        .addIntents(Intent.MESSAGE_CONTENT)
        .login()
        .join();

      // Set to online and set status
      refreshStatus();

      // Set up slash commands
      api.addSlashCommandCreateListener(discordMessageHandler::processSlashCommand);
      Set<SlashCommandBuilder> slashCommands = DiscordSlashCommands.getSlashCommands();
      api.bulkOverwriteGlobalApplicationCommands(slashCommands).join();

      // Done
      LOGGER.info("Successfully connected ListeningPartyBot to Discord!");
      LOGGER.info("Servers: " + api.getServers().size());
    } catch (Exception e) {
      LOGGER.error("Failed to start bot! (Couldn't read Discord API token.) Terminating...");
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
  void refreshStatus() {
    if (api != null) {
      api.updateStatus(UserStatus.ONLINE);
      int serverCount = api.getServers().size();
      api.updateActivity(ActivityType.LISTENING, String.format("/help | %d server%s | Listening Parties", serverCount, serverCount != 1 ? "s" : ""));
    }
  }
}
