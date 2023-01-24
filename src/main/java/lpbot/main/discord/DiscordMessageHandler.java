package lpbot.main.discord;

import java.util.List;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lpbot.main.discord.util.Digester;
import lpbot.main.discord.util.SpamProtector;
import lpbot.main.party.LPHandler;
import lpbot.main.playlist.TotwDataHandler;
import lpbot.main.playlist.LPEntity;
import lpbot.main.spotify.api.services.PlaylistService;

@Component
public class DiscordMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMessageHandler.class);

  public final static String PREFIX = "!lp";

  private final TotwDataHandler lpDataHandler;
  private final LPHandler lpPartyHandler;

  public DiscordMessageHandler(TotwDataHandler lpDataHandler, LPHandler lpPartyHandler) {
    this.lpDataHandler = lpDataHandler;
    this.lpPartyHandler = lpPartyHandler;
  }

  public void processMessage(MessageCreateEvent message) {
    // TODO multi-channel support for multiple sessions at once. Session is linked to channel (stored via HashMap). Only the starter of the playlist has the ability to stop it
    if (SpamProtector.checkAuthorOkay(message.getMessageAuthor())) {
      String content = message.getMessageContent();
      if (content.startsWith(PREFIX)) {
        LOGGER.debug("New potential message received: " + content);

        TextChannel channel = message.getChannel();
        Digester messageDigester = new Digester(content.substring(PREFIX.length()));
        String firstWord = messageDigester.shift();
        switch (firstWord) {
          case "start":
            lpPartyHandler.start(channel);
            break;
          case "stop":
            lpPartyHandler.stop(channel);
            break;
          case "pause":
            lpPartyHandler.pause(channel);
            break;
          case "resume":
            lpPartyHandler.resume(channel);
            break;
          case "link":
            String linkForChannel = lpPartyHandler.getLinkForChannel(channel);
            if (linkForChannel != null) {
              channel.sendMessage(linkForChannel);
            } else {
              channel.sendMessage("**There's currently no link set for this channel! Use `!lp set <link>` to set it**");
            }
            break;
          case "set":
            String potentialPlaylist = messageDigester.shift();
            if (potentialPlaylist != null && !potentialPlaylist.isBlank()) {
              boolean success = lpPartyHandler.setLink(potentialPlaylist);
              if (success) {
                channel.sendMessage("Listening Party link set! Use `!lp start` to begin the session");
                return;
              }
            }
            channel.sendMessage("Usage: `!lp set <link>` (Spotify album or playlist)");
            break;
          case "totw":
            // TODO totw
//            int updatedSongCount = createOrRefreshListeningPartyPlaylist();
//            message.getChannel().sendMessage("LP playlist updated! New song count: " + updatedSongCount);
            break;
          case "help":
            // TODO update help (perhaps autogenerate)
            EmbedBuilder helpEmbed = new EmbedBuilder();
            helpEmbed.setTitle("Listening Party Bot - Help");
            helpEmbed.addField("`!lp start`", "Start the Listening Party (beginning with a countdown)");
            helpEmbed.addField("`!lp stop`", "Cancel a currently ongoing Listening Party and reset it to the beginning");
            helpEmbed.addField("`!lp playlist`", "Print the set playlist link");
            helpEmbed.addField("`!lp setplaylist`", "Set the playlist link (must be provided as pure Spotify ID)");
            helpEmbed.addField("`!lp refreshplaylist`", "Refresh the Spotify playlist with the current Google Forms data (requires a valid playlist link to be set)");
            helpEmbed.addField("`!lp help`", "Display this page");
            channel.sendMessage(helpEmbed);
            break;
          default:
            EmbedBuilder basicUsageEmbed = new EmbedBuilder();
            basicUsageEmbed.setTitle("Usage");
            basicUsageEmbed.setDescription("`!lp <command>`\n\nSee `!lp help` for more information");
            channel.sendMessage(basicUsageEmbed);
            break;
        }
      }
    }
  }
}
