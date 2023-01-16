package totwbot.main.discord;

import java.util.List;
import java.util.stream.Collectors;

import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.NumberUtils;

import com.google.common.primitives.Ints;

import totwbot.main.playlist.TotwDataHandler;
import totwbot.main.playlist.TotwEntity;
import totwbot.main.party.TotwPartyHandler;
import totwbot.spotify.api.services.PlaylistService;
import totwbot.main.discord.util.Digester;
import totwbot.main.discord.util.SpamProtector;

@Component
public class DiscordMessageHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMessageHandler.class);

  public final static String PREFIX = "!totw.";

  @Autowired
  private TotwDataHandler totwDataHandler;

  @Autowired
  private TotwPartyHandler totwEventHandler;

  @Autowired
  private PlaylistService playlistService;

  private String playlistId = null;

  public void processMessage(MessageCreateEvent message) {
    if (SpamProtector.checkAuthorOkay(message.getMessageAuthor())) {
      String content = message.getMessageContent();
      if (content.startsWith(PREFIX)) {
        LOGGER.debug("New potential message received: " + content);

        Digester messageDigester = new Digester(content.substring(PREFIX.length()));
        String firstWord = messageDigester.shift();
        switch (firstWord) {
          case "start":
            totwEventHandler.start(message.getChannel());
            break;
          case "pause":
            // todo
            break;
          case "resume":
            // todo
            break;
          case "stop":
            if (totwEventHandler.isStarted()) {
              totwEventHandler.stop();
              // todo stop everything
            }
            break;
          case "skip":
            int skipCount = 1;
            String shift = messageDigester.shift();
            if (shift != null && Ints.tryParse(shift) != null) {
              skipCount = Ints.tryParse(shift);
            }
            // todo do the actual skipping
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
              message.getChannel().sendMessage("Usage: `!totw.setplaylist PLAYLISTID`");
            }
            break;
          case "playlistlink":
            if (playlistId != null) {
              message.getChannel().sendMessage(playlistLink());
            } else {
              message.getChannel().sendMessage("**There's currently no playlist set! Use `!totw.setplaylist` to set it.**");
            }
            break;
          case "refreshplaylist":
            // TODO get the data from Google Forms
            int updatedSongCount = createOrRefreshTotwPlaylist();
            message.getChannel().sendMessage("TOTW playlist updated! New song count: " + updatedSongCount);
            break;
          case "preview":
            break; // todo
          case "help":
            break; // todo
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
