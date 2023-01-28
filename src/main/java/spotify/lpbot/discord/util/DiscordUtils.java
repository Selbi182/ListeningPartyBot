package spotify.lpbot.discord.util;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class DiscordUtils {
  private DiscordUtils() {
  }

  ////////////////
  // Builders

  /**
   * Create a simple embed with only the description set
   */
  public static EmbedBuilder createSimpleEmbed(String content) {
    return createSimpleEmbed(content, null, false);
  }

  /**
   * Create a simple embed with only the description set
   */
  public static EmbedBuilder createSimpleEmbed(String content, boolean useTitle) {
    return createSimpleEmbed(content, null, useTitle);
  }

  /**
   * Create a simple embed with only the description and color set
   */
  public static EmbedBuilder createSimpleEmbed(String content, Color color, boolean useTitle) {
    EmbedBuilder embed = new EmbedBuilder();
    if (useTitle) {
      embed.setTitle(content);
    } else {
      embed.setDescription(content);
    }
    embed.setColor(color);
    return embed;
  }

  /**
   * Create a simple embed with only the description. The color red is predefined
   */
  public static EmbedBuilder createErrorEmbed(String content) {
    return createSimpleEmbed("**ERROR:** " + content, Color.RED, false);
  }

  ////////////////
  // Send Embed

  /**
   * Create and send a simple embed to the given channel
   */
  public static void sendSimpleEmbed(TextChannel textChannel, String content) {
    textChannel.sendMessage(createSimpleEmbed(content));
  }

  ////////////////
  // Update Embed

  /**
   * Respond the given InteractionOriginalResponseUpdater with a simple embed
   */
  public static void updateWithSimpleEmbed(InteractionOriginalResponseUpdater responder, String content) {
    respondWithEmbed(responder, createSimpleEmbed(content));
  }

  /**
   * Respond the given InteractionOriginalResponseUpdater with an error embed
   */
  public static void updateWithErrorEmbed(InteractionOriginalResponseUpdater responder, String content) {
    respondWithEmbed(responder, createErrorEmbed(content));
  }

  ////////////////
  // Response

  /**
   * Respond to the given InteractionOriginalResponseUpdater with a simple message
   * @return the updated message in case further stuff needs to be done with it
   */
  public static CompletableFuture<Message> respondWithMessage(InteractionOriginalResponseUpdater responder, String message) {
    return responder.setContent(message).update();
  }

  /**
   * Respond with a custom embed
   * @return the updated message in case further stuff needs to be done with it
   */
  public static CompletableFuture<Message> respondWithEmbed(InteractionOriginalResponseUpdater responder, EmbedBuilder embed) {
    return responder.addEmbed(embed).update();
  }

}
