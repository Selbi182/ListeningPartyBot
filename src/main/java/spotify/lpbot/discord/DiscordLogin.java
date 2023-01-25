package spotify.lpbot.discord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
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

  public DiscordLogin(DiscordMessageHandler discordMessageHandler) {
    this.discordMessageHandler = discordMessageHandler;
  }

  @EventListener(LoggedInEvent.class)
  public void start() {
    LOGGER.info("Successfully logged into Spotify!");
    try {
      LOGGER.info("Connecting ListeningPartyBot to Discord...");
      String token = readToken();
      DiscordApi api = new DiscordApiBuilder()
          .setToken(token)
          .addIntents(Intent.MESSAGE_CONTENT)
          .login()
          .join();
      api.updateStatus(UserStatus.ONLINE);
      api.addSlashCommandCreateListener(discordMessageHandler::processSlashCommand);
      setupSlashCommands(api);
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

  private void setupSlashCommands(DiscordApi api) {
    Set<SlashCommandBuilder> commands = new HashSet<>(List.of(
      SlashCommand.with("start", "Start the Listening Party", List.of(SlashCommandOption.create(SlashCommandOptionType.LONG, "countdown", "the seconds to count down (default 5)", false))),
      SlashCommand.with("stop", "Cancel a currently ongoing Listening Party and reset it to the beginning"),
      SlashCommand.with("status", "Print info of the current Listening Party for this channel"),
      SlashCommand.with("link", "Print the set album/playlist link"),
      SlashCommand.with("set", "Set the album/playlist link (must be provided as URL)", List.of(SlashCommandOption.create(SlashCommandOptionType.STRING, "url", "the URL to the Spotify playlist or album", true))),
      SlashCommand.with("help", "Print the commands as chat message"),
      SlashCommand.with("totw", "[Restricted Access] Host a Track-of-the-Week party", List.of(SlashCommandOption.create(SlashCommandOptionType.ATTACHMENT, "attachment", "the TOTW info data", true)))
    ));
    api.bulkOverwriteGlobalApplicationCommands(commands).join();
  }
}
