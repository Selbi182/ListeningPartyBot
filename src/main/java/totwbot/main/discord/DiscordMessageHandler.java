package totwbot.main.discord;

import java.util.List;
import java.util.stream.Collectors;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import totwbot.main.discord.util.Digester;
import totwbot.main.discord.util.SpamProtector;
import totwbot.main.party.TotwPartyHandler;
import totwbot.main.playlist.TotwDataHandler;
import totwbot.main.playlist.TotwEntity;
import totwbot.main.spotify.api.services.PlaylistService;

@Component
public class DiscordMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMessageHandler.class);

  public final static String PREFIX = "!totw";

  private final TotwDataHandler totwDataHandler;
  private final TotwPartyHandler totwPartyHandler;
  private final PlaylistService playlistService;

  private String playlistId = null;

  public DiscordMessageHandler(TotwDataHandler totwDataHandler, TotwPartyHandler totwPartyHandler, PlaylistService playlistService) {
    this.totwDataHandler = totwDataHandler;
    this.totwPartyHandler = totwPartyHandler;
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
            totwPartyHandler.start(message.getChannel());
            break;
          case "skip":
            totwPartyHandler.skip();
            break;
          case "stop":
            totwPartyHandler.stop();
            break;
          case "playlist":
            if (playlistId != null) {
              message.getChannel().sendMessage(playlistLink());
            } else {
              message.getChannel().sendMessage("**There's currently no playlist set! Use `!totw setplaylist` to set it.**");
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
              message.getChannel().sendMessage("Usage: `!totw setplaylist PLAYLISTID`");
            }
            break;
          case "refreshplaylist":
            // TODO get the data from Google Forms
            int updatedSongCount = createOrRefreshTotwPlaylist();
            message.getChannel().sendMessage("TOTW playlist updated! New song count: " + updatedSongCount);
            break;
          case "help":
            EmbedBuilder helpEmbed = new EmbedBuilder();
            helpEmbed.setTitle("Help");
            helpEmbed.addField("`!totw start`", "Start the TOTW session (beginning with a countdown)");
            helpEmbed.addField("`!totw skip`", "Skip the current song and immediately jump to the next one in the queue");
            helpEmbed.addField("`!totw stop`", "Cancel a currently ongoing TOTW session and reset it to the beginning");
            helpEmbed.addField("`!totw playlist`", "Print the set playlist link");
            helpEmbed.addField("`!totw setplaylist`", "Set the playlist link (must be provided as pure Spotify ID).");
            helpEmbed.addField("`!totw refreshplaylist`", "Refresh the Spotify playlist with the current Google Forms data (requires a valid playlist link to be set)");
            helpEmbed.addField("`!totw help`", "Display this page");
            message.getChannel().sendMessage(helpEmbed);
            break;
          default:
            EmbedBuilder basicUsageEmbed = new EmbedBuilder();
            basicUsageEmbed.setTitle("Usage");
            basicUsageEmbed.setDescription("`!totw <command>`\n\nSee `!totw help` for more information.");
            message.getChannel().sendMessage(basicUsageEmbed);
            break;
        }
      }
    }
  }

  private int createOrRefreshTotwPlaylist() {
    List<String> songIds = totwDataHandler.getTotwEntityList().stream()
        .map(TotwEntity::getSongId)
        .collect(Collectors.toList());

    playlistService.clearPlaylist(playlistId);
    playlistService.addSongsToPlaylistById(playlistId, songIds);

    return songIds.size();
  }

  private String playlistLink() {
    return "https://open.spotify.com/playlist/" + playlistId;
  }
}
