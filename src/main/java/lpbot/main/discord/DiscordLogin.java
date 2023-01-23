package lpbot.main.discord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

import lpbot.main.spotify.api.events.LoggedInEvent;

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
      api.addMessageCreateListener(discordMessageHandler::processMessage);
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
