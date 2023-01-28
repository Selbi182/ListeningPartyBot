package spotify.lpbot.discord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

import spotify.api.events.LoggedInEvent;

@Component
public class DiscordLogin {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordLogin.class);

  private final DiscordMessageHandler discordMessageHandler;

  DiscordLogin(DiscordMessageHandler discordMessageHandler) {
    this.discordMessageHandler = discordMessageHandler;
  }

  @EventListener(LoggedInEvent.class)
  public void start() {
    LOGGER.info("Successfully logged into Spotify!");
    try {
      LOGGER.info("Connecting ListeningPartyBot to Discord...");

      // Read the Discord API token
      String token = readToken();

      // Login
      DiscordApi api = new DiscordApiBuilder()
          .setToken(token)
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
      LOGGER.error("Failed to start bot! (Couldn't read Discord token.) Terminating...");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private String readToken() throws IOException {
    File tokenFile = new File("./discordtoken.txt");
    if (tokenFile.canRead()) {
      return Files.asCharSource(tokenFile, Charset.defaultCharset()).readFirstLine();
    }
    throw new IOException("Can't read token file!");
  }
}
