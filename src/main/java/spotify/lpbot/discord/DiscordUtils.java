package spotify.lpbot.discord;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

public class DiscordUtils {
  private DiscordUtils() {}

  /**
   * Print the given message to the given channel in bold
   */
  public static void sendMessage(TextChannel channel, String text) {
    channel.sendMessage("**" + text + "**");
  }

  /**
   * Respond to the given responder with the given text
   */
  public static void sendResponse(InteractionImmediateResponseBuilder responder, String text) {
    responder.setContent("**" + text + "**").respond();
  }
}
