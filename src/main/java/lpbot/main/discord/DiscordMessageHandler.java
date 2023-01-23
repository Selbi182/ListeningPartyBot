package lpbot.main.discord;

import java.util.List;
import java.util.stream.Collectors;

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
  private final PlaylistService playlistService;

  private String playlistId = null;

  public DiscordMessageHandler(TotwDataHandler lpDataHandler, LPHandler lpPartyHandler, PlaylistService playlistService) {
    this.lpDataHandler = lpDataHandler;
    this.lpPartyHandler = lpPartyHandler;
    this.playlistService = playlistService;
  }

  public void processMessage(MessageCreateEvent message) {
    if (SpamProtector.checkAuthorOkay(message.getMessageAuthor())) {
      String content = message.getMessageContent();
      if (content.startsWith(PREFIX)) {
        LOGGER.debug("New potential message received: " + content);

        Digester messageDigester = new Digester(content.substring(PREFIX.length()));
        String firstWord = messageDigester.shift();
        switch (firstWord) {
          case "start":
            lpPartyHandler.start(message.getChannel());
            break;
          case "stop":
            lpPartyHandler.stop();
            break;
          case "playlist":
            if (playlistId != null) {
              message.getChannel().sendMessage(playlistLink());
            } else {
              message.getChannel().sendMessage("**There's currently no playlist set! Use `!lp setplaylist` to set it.**");
            }
            break;
          case "setplaylist":
            String potentialPlaylistId = messageDigester.shift();
            if (potentialPlaylistId != null && !potentialPlaylistId.isBlank()) {
              if (playlistService.isValidPlaylistId(potentialPlaylistId)) {
                this.playlistId = potentialPlaylistId;
                message.getChannel().sendMessage("Set target playlist to: " + playlistLink());
              } else {
                message.getChannel().sendMessage(potentialPlaylistId + " is not a valid Spotify playlist ID! Make sure to only provide the actual ID, not anything else from the URL.");
              }
            } else {
              message.getChannel().sendMessage("Usage: `!lp setplaylist PLAYLISTID`");
            }
            break;
          case "refreshplaylist":
            // TODO get the data from Google Forms
            int updatedSongCount = createOrRefreshListeningPartyPlaylist();
            message.getChannel().sendMessage("LP playlist updated! New song count: " + updatedSongCount);
            break;
          case "help":
            EmbedBuilder helpEmbed = new EmbedBuilder();
            helpEmbed.setTitle("Help");
            helpEmbed.addField("`!lp start`", "Start the Listening Party (beginning with a countdown)");
            helpEmbed.addField("`!lp stop`", "Cancel a currently ongoing Listening Party and reset it to the beginning");
            helpEmbed.addField("`!lp playlist`", "Print the set playlist link");
            helpEmbed.addField("`!lp setplaylist`", "Set the playlist link (must be provided as pure Spotify ID).");
            helpEmbed.addField("`!lp refreshplaylist`", "Refresh the Spotify playlist with the current Google Forms data (requires a valid playlist link to be set)");
            helpEmbed.addField("`!lp help`", "Display this page");
            message.getChannel().sendMessage(helpEmbed);
            break;
          default:
            EmbedBuilder basicUsageEmbed = new EmbedBuilder();
            basicUsageEmbed.setTitle("Usage");
            basicUsageEmbed.setDescription("`!lp <command>`\n\nSee `!lp help` for more information.");
            message.getChannel().sendMessage(basicUsageEmbed);
            break;
        }
      }
    }
  }

  private int createOrRefreshListeningPartyPlaylist() {
    List<String> songIds = lpDataHandler.getLPEntityList().stream()
        .map(LPEntity::getSongId)
        .collect(Collectors.toList());

    playlistService.clearPlaylist(playlistId);
    playlistService.addSongsToPlaylistById(playlistId, songIds);

    return songIds.size();
  }

  private String playlistLink() {
    return "https://open.spotify.com/playlist/" + playlistId;
  }
}
