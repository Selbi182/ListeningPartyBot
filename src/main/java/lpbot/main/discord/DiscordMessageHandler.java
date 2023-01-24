package lpbot.main.discord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import lpbot.main.discord.util.Digester;
import lpbot.main.discord.util.SpamProtector;
import lpbot.main.party.data.LPInstance;
import lpbot.main.party.service.ChannelRegistry;
import lpbot.main.party.totw.TotwDataHandler;
import lpbot.main.party.totw.TotwEntity;
import lpbot.main.spotify.util.BotUtils;

@Component
public class DiscordMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMessageHandler.class);

  public final static String PREFIX = "&lp"; // TODO replace with registered slash commands

  private final ChannelRegistry lpChannelRegistry;

  public DiscordMessageHandler(ChannelRegistry lpChannelRegistry) {
     this.lpChannelRegistry = lpChannelRegistry;
  }

  public void processMessage(MessageCreateEvent message) {
    if (SpamProtector.checkAuthorOkay(message.getMessageAuthor())) {
      String content = message.getMessageContent();
      if (content.startsWith(PREFIX)) {
        LOGGER.debug("New potential message received: " + content);

        TextChannel channel = message.getChannel();
        Optional<LPInstance> lpInstance = lpChannelRegistry.getExistingLPInstance(channel);

        Digester messageDigester = new Digester(content.substring(PREFIX.length()));
        String firstWord = messageDigester.shift();
        switch (firstWord) {
          // LPInstance setup
          case "set":
            String potentialPlaylist = messageDigester.shift();
            if (potentialPlaylist != null && !potentialPlaylist.isBlank()) {
              LPInstance registeredInstance = lpChannelRegistry.register(channel, potentialPlaylist);
              if (registeredInstance != null) {
                BotUtils.sendMessage(channel, "Listening Party link set! Use `"+ PREFIX + " start` to begin the session");
              } else {
                BotUtils.sendMessage(channel, "ERROR: Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
              }
            } else {
              BotUtils.sendMessage(channel, "Usage: `"+ PREFIX + " set <link>` (Spotify album or playlist)");
            }
            break;

          case "link":
            lpInstance.ifPresentOrElse((lp) -> BotUtils.sendMessage(channel, lp.getAlbumOrPlaylistUri()), () -> sendGenericUnsetError(channel));
            break;

          // LPInstance control
          case "start":
            int countdown = 5;
            String customCountdownSeconds = messageDigester.shift();
            if (customCountdownSeconds != null && !customCountdownSeconds.isBlank()) {
              try {
                countdown = Integer.parseInt(customCountdownSeconds);
                if (countdown < 1) {
                  throw new NumberFormatException();
                }
              } catch (NumberFormatException e) {
                BotUtils.sendMessage(channel, customCountdownSeconds + " isn't a valid positive number");
                return;
              }
            }
            final int finalCountdown = countdown;
            lpInstance.ifPresentOrElse(lp -> lp.start(finalCountdown), () -> sendGenericUnsetError(channel));
            break;

          case "stop":
            lpInstance.ifPresentOrElse(lp -> {
              lp.stop();
              BotUtils.sendMessage(channel, "Listening Party cancelled!");
            }, () -> sendGenericUnsetError(channel));
            break;

          case "status":
            lpInstance.ifPresentOrElse(LPInstance::status, () -> sendGenericUnsetError(channel));
            break;

          // TOTW
          case "totw":
            // TODO for now only I got the power lol
            final long SELBI_ID = 186507215807447041L;
            if (message.getMessageAuthor().getId() == SELBI_ID) {
              List<MessageAttachment> attachments = message.getMessageAttachments();
              if (!attachments.isEmpty() && !attachments.get(0).isImage()) {
                try {
                  String fileContent = StreamUtils.copyToString(attachments.get(0).getUrl().openStream(), StandardCharsets.UTF_8);
                  TotwEntity totwData = TotwDataHandler.parseTextFile(fileContent);
                  LPInstance registeredInstance = lpChannelRegistry.registerTotw(channel, totwData);
                  if (registeredInstance != null) {
                    channel.sendMessage("**TOTW party is set (" + totwData.getParticipants() + " participants)! Use `"+ PREFIX + " start` to begin the session**\n" + registeredInstance.getAlbumOrPlaylistUri());
                  } else {
                    BotUtils.sendMessage(channel, "ERROR: Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
                  }
                } catch (IOException | RuntimeException e) {
                  BotUtils.sendMessage(channel, "Invalid TOTW data format.");
                  e.printStackTrace();
                }
              }
            } else {
              BotUtils.sendMessage(channel, "Access denied.");
            }
            break;

          // Other
          case "help":
            sendHelpEmbed(channel);
            break;
          default:
            sendBasicUsageEmbed(channel);
            break;
        }
      }
    }
  }

  private void sendGenericUnsetError(TextChannel channel) {
    BotUtils.sendMessage(channel, "There's currently no Listening Party set for this channel! Use `"+ PREFIX + " set <link>` to set it");
  }

  private void sendHelpEmbed(TextChannel channel) {
    EmbedBuilder helpEmbed = new EmbedBuilder();
    helpEmbed.setTitle("Listening Party Bot - Help");
    helpEmbed.addField("`" + PREFIX + " start <countdown>`", "Start the Listening Party (`countdown` can be set manually in seconds, defaults to 5)");
    helpEmbed.addField("`" + PREFIX + " status`", " While a Listening Party is ongoing, prints the current song with timestamp (so that late joiners aren't left behind)");
    helpEmbed.addField("`" + PREFIX + " stop`", "Cancel a currently ongoing Listening Party and reset it to the beginning");
    helpEmbed.addField("`" + PREFIX + " link`", "Print the set album/playlist link");
    helpEmbed.addField("`" + PREFIX + " set`", " Set the album/playlist link (must be provided as URL)");
    helpEmbed.addField("`" + PREFIX + " help`", "Display this page");
    helpEmbed.addField("`" + PREFIX + " totw`", "[Restricted Access] Host a Track-of-the-Week party. This feature is only available to trusted users");
    channel.sendMessage(helpEmbed);
  }

  private void sendBasicUsageEmbed(TextChannel channel) {
    EmbedBuilder basicUsageEmbed = new EmbedBuilder();
    basicUsageEmbed.setTitle("Usage");
    basicUsageEmbed.setDescription("`"+ PREFIX + " <command>`\n\nSee `"+ PREFIX + " help` for more information");
    channel.sendMessage(basicUsageEmbed);
  }
}
