package spotify.lpbot.discord.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

public class DiscordUtils {
  private DiscordUtils() {
  }

  /**
   * The URL homepage for the bot
   */
  public static final String LPBOT_URL = "lpbot.selbi.club";

  /**
   * The URL homepage for the bot (with https:// prefix)
   */
  public static final String LPBOT_URL_HTTPS = "https:// " + LPBOT_URL;

  ////////////////
  // Builders

  /**
   * Create a simple embed with only the description set
   */
  public static EmbedBuilder createSimpleEmbed(String content) {
    return createSimpleEmbed(content, false, null);
  }

  /**
   * Create a simple embed with only the description set
   */
  public static EmbedBuilder createSimpleEmbed(String content, boolean useTitle) {
    return createSimpleEmbed(content, useTitle, null);
  }

  /**
   * Create a simple embed with only the description and color set
   */
  public static EmbedBuilder createSimpleEmbed(String content, boolean useTitle, Color color) {
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
    return createSimpleEmbed("**ERROR:** " + content, false, Color.RED);
  }

  ////////////////
  // Send Embed

  /**
   * Create and send a simple message to the given channel
   */
  public static void sendSimpleMessage(TextChannel textChannel, String content) {
    textChannel.sendMessage(content);
  }

  /**
   * Create and send a simple embed to the given channel
   */
  public static void sendSimpleEmbed(TextChannel textChannel, String content) {
    textChannel.sendMessage(createSimpleEmbed(content));
  }

  ////////////////
  // Update Embed

  /**
   * Respond to the given InteractionOriginalResponseUpdater with a simple embed
   */
  public static void updateWithSimpleEmbed(InteractionOriginalResponseUpdater responder, String content) {
    respondWithEmbed(responder, createSimpleEmbed(content));
  }

  /**
   * Respond to the given InteractionOriginalResponseUpdater with an error embed
   */
  public static void updateWithErrorEmbed(InteractionOriginalResponseUpdater responder, String content) {
    respondWithEmbed(responder, createErrorEmbed(content));
  }

  /**
   * Respond to the given InteractionOriginalResponseUpdater with an embed that also has content
   */
  public static void updateWithContentEmbed(InteractionOriginalResponseUpdater responder, String embedContent, String mainContent) {
    responder.setContent(mainContent).addEmbed(createSimpleEmbed(embedContent)).update();
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

  ////////////////
  // Misc

  /**
   * Format the given headline and content in a pretty way for the description part of embeds
   * @param headline the headline
   * @param content the main content
   * @return the formatted string
   */
  public static String formatDescription(String headline, String content) {
    String descriptionBody = Arrays.stream(content.split("\n"))
        .map(String::trim)
        .collect(Collectors.joining("\n> "));
    return String.format("**%s:**\n> *%s*", headline, descriptionBody);
  }
}
