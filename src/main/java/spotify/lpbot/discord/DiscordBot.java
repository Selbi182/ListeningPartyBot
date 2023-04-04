package spotify.lpbot.discord;

import java.util.Set;

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
import org.springframework.stereotype.Component;

import spotify.api.events.SpotifyApiLoggedInEvent;

@Component
public class DiscordBot {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordBot.class);

  private final DiscordMessageHandler discordMessageHandler;

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
      DiscordApi api = new DiscordApiBuilder()
        .setToken(discordApiToken)
        .addIntents(Intent.MESSAGE_CONTENT)
        .login()
        .join();

      // Set to online and set status
      api.updateStatus(UserStatus.ONLINE);
      api.updateActivity(ActivityType.LISTENING, "Listening Parties");

      // Set up slash commands
      api.addSlashCommandCreateListener(discordMessageHandler::processSlashCommand);
      Set<SlashCommandBuilder> slashCommands = DiscordSlashCommands.getSlashCommands();
      api.bulkOverwriteGlobalApplicationCommands(slashCommands).join();

      // Done
      LOGGER.info("Successfully connected ListeningPartyBot to Discord!");
    } catch (Exception e) {
      LOGGER.error("Failed to start bot! (Couldn't read Discord API token.) Terminating...");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
