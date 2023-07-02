package spotify.lpbot.discord.util;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import spotify.lpbot.discord.DiscordSlashCommands;

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
  public static final String LPBOT_URL_HTTPS = "https://" + LPBOT_URL;

  /**
   * The max length of a description in a Discord embed
   */
  private static final int DESCRIPTION_EMBED_MAX_LENGTH = 2048;


  ////////////////
  // Command formatting

  /**
   * Return the given command name and ID in the format required for a clickable command.
   * If the ID is null, a simple code-string will be returned.
   */
  public static String asClickableCommand(String commandName, Long commandId) {
    if (commandId != null) {
      return String.format("</%s:%d>", commandName, commandId);
    }
    return String.format("`/%s`", commandName);
  }

  /**
   * Find and return the given command name as clickable command.
   */
  public static String findClickableCommand(String commandName) {
    return DiscordSlashCommands.getLpBotCommands().stream()
      .filter(command -> command.getCommand().equals(commandName))
      .findFirst()
      .map(command -> asClickableCommand(commandName,command.getId()))
      .orElse(asClickableCommand(commandName, null));
  }

  /**
   * Truncate the given text to the max limit of Discord embeds, should it exceed it (2048).
   * If truncated, ... will be added.
   *
   * @param str the input string
   * @param buffer an additional buffer on top of the max limit to specify
   * @return the (potentially) truncated string
   */
  public static String truncateToMaxDescription(String str, int buffer) {
    int limit = DESCRIPTION_EMBED_MAX_LENGTH - buffer;
    if (str.length() <= limit) {
      return str;
    }
    int lastSpace = str.lastIndexOf(" ", limit - 4);
    if (lastSpace != -1) {
      return str.substring(0, lastSpace).strip() + "\u2026";
    }
    return str.substring(0, limit - 3).strip() + "\u2026";
  }

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

  ////////////////
  // Response

  /**
   * Respond to the given InteractionOriginalResponseUpdater with a simple message
   *
   */
  public static Message respondWithMessage(InteractionOriginalResponseUpdater responder, String message) {
    return responder.setContent(message).update().join();
  }

  /**
   * Respond with a custom embed
   *
   * @return the updated message in case further stuff needs to be done with it
   */
  public static Message respondWithEmbed(InteractionOriginalResponseUpdater responder, EmbedBuilder embed) {
    return responder.addEmbed(embed).update().join();
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
