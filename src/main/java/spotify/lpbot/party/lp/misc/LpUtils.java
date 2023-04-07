package spotify.lpbot.party.lp.misc;

import java.util.logging.Logger;

import org.javacord.api.entity.channel.TextChannel;

public final class LpUtils {
  private LpUtils() {}

  public static void logLpEvent(TextChannel textChannel, Logger logger, String message) {
    textChannel.asServerChannel()
      .ifPresent(channel -> logger.info(String.format("%s (#%s) -- %s", channel.getServer().getName(), channel.getName(), message)));
  }
}
