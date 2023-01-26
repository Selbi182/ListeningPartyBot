package spotify.lpbot.discord;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

public class DiscordUtils {
  private DiscordUtils() {}

  /**
   * Print the given message to the given channel in bold
   */
  public static void sendMessage(TextChannel channel, String text) {
    sendMessage(channel, text, true);
  }

  /**
   * Print the given message to the given channel, with an optional flag to make it bold or not
   */
  public static void sendMessage(TextChannel channel, String text, boolean bold) {
    String content = bold ? "**" + text + "**" : text;
    channel.sendMessage(content);
  }

  /**
   * Respond to the given responder with the given text in bold
   */
  public static void sendResponse(InteractionImmediateResponseBuilder responder, String text) {
    sendResponse(responder, text, true);
  }

  /**
   * Respond to the given responder with the given text, with an optional flag to make it bold or not
   */
  public static void sendResponse(InteractionImmediateResponseBuilder responder, String text, boolean bold) {
    String content = bold ? "**" + text + "**" : text;
    responder.setContent(content).respond();
  }
}
