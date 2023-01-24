package lpbot.main.discord;

import java.util.Optional;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lpbot.main.discord.util.Digester;
import lpbot.main.discord.util.SpamProtector;
import lpbot.main.party.LPChannelRegistry;
import lpbot.main.party.LPInstance;

@Component
public class DiscordMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMessageHandler.class);

  public final static String PREFIX = "!lp";

  private final LPChannelRegistry lpChannelRegistry;

  public DiscordMessageHandler(LPChannelRegistry lpChannelRegistry) {
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
                sendMessage(channel, "Listening Party link set! Use `!lp start` to begin the session");
              } else {
                sendMessage(channel, "ERROR: Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
              }
            } else {
              sendMessage(channel, "Usage: `!lp set <link>` (Spotify album or playlist)");
            }
            break;

          case "link":
            lpInstance.ifPresentOrElse((lp) -> sendMessage(channel, lp.getAlbumOrPlaylistUri()), () -> sendGenericUnsetError(channel));
            break;

          // LPInstance control
          case "start":
            int countdown = 10;
            String customCountdownSeconds = messageDigester.shift();
            if (customCountdownSeconds != null && !customCountdownSeconds.isBlank()) {
              try {
                countdown = Integer.parseInt(customCountdownSeconds);
                if (countdown < 1) {
                  throw new NumberFormatException();
                }
              } catch (NumberFormatException e) {
                sendMessage(channel, customCountdownSeconds + " isn't a valid positive number");
                return;
              }
            }
            final int finalCountdown = countdown;
            lpInstance.ifPresentOrElse(lp -> lp.start(finalCountdown), () -> sendGenericUnsetError(channel));
            break;
          case "stop":
            lpInstance.ifPresentOrElse(lp -> {
              lp.stop();
              sendMessage(channel, "**Listening Party cancelled!**");
            }, () -> sendGenericUnsetError(channel));
            break;

//          case "totw":
//            // TODO totw
////            int updatedSongCount = createOrRefreshListeningPartyPlaylist();
////            message.getChannel().sendMessage("LP playlist updated! New song count: " + updatedSongCount);
//            break;

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
    channel.sendMessage("**There's currently no link set for this channel! Use `!lp set <link>` to set it**");
  }

  private void sendMessage(TextChannel channel, String text) {
    channel.sendMessage("**" + text + "**");
  }

  private void sendHelpEmbed(TextChannel channel) {
    // TODO update help (perhaps autogenerate)
    EmbedBuilder helpEmbed = new EmbedBuilder();
    helpEmbed.setTitle("Listening Party Bot - Help");
    helpEmbed.addField("`!lp start <countdown>`", "Start the Listening Party (`countdown` can be set manually in seconds, defaults to 10");
    helpEmbed.addField("`!lp stop`", "Cancel a currently ongoing Listening Party and reset it to the beginning");
    helpEmbed.addField("`!lp playlist`", "Print the set playlist link");
    helpEmbed.addField("`!lp setplaylist`", "Set the playlist link (must be provided as pure Spotify ID)");
    helpEmbed.addField("`!lp refreshplaylist`", "Refresh the Spotify playlist with the current Google Forms data (requires a valid playlist link to be set)");
    helpEmbed.addField("`!lp help`", "Display this page");
    channel.sendMessage(helpEmbed);
  }

  private void sendBasicUsageEmbed(TextChannel channel) {
    EmbedBuilder basicUsageEmbed = new EmbedBuilder();
    basicUsageEmbed.setTitle("Usage");
    basicUsageEmbed.setDescription("`!lp <command>`\n\nSee `!lp help` for more information");
    channel.sendMessage(basicUsageEmbed);
  }
}
