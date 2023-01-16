package totwbot.main.discord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;

import totwbot.spotify.api.events.LoggedInEvent;

@Component
public class DiscordLogin {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordLogin.class);

  @Autowired
  private DiscordMessageHandler totwEventHandler;

  @EventListener(LoggedInEvent.class)
  public void start() {
    LOGGER.info("Successfully logged into Spotify!");
    try {
      LOGGER.info("Connecting TotwBot to Discord...");
      String token = readToken();
      DiscordApi api = new DiscordApiBuilder()
          .setToken(token)
          .addIntents(Intent.MESSAGE_CONTENT)
          .login()
          .join();
      api.updateStatus(UserStatus.ONLINE);
      api.addMessageCreateListener(totwEventHandler::processMessage);
      LOGGER.info("Successfully connected TotwBot to Discord!");
    } catch (Exception e) {
      LOGGER.error("Failed to start bot! Terminating...");
      e.printStackTrace();
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
