package lpbot.main.party;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Component;

import com.google.common.collect.Iterables;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;
import lpbot.main.lastfm.LastFmDataHandler;
import lpbot.main.lastfm.LPEntityWithLastFmData;
import lpbot.main.playlist.TotwDataHandler;
import lpbot.main.playlist.LPEntity;
import lpbot.main.spotify.api.SpotifyCall;
import lpbot.main.spotify.util.BotUtils;

@Component
public class LPHandler {
  private final Color EMBED_COLOR = new Color(173, 20, 87);

  private final SpotifyApi spotifyApi;
  private final TotwDataHandler lpDataHandler;
  private final LastFmDataHandler lastFmDataHandler;

  private boolean started;
  private List<LPEntity> lpEntities;
  private TextChannel channel;

  private final SingleTaskWaitingExecutor singleTaskWaitingExecutor;

  public LPHandler(SpotifyApi spotifyApi, TotwDataHandler lpDataHandler, LastFmDataHandler lastFmDataHandler) {
    this.spotifyApi = spotifyApi;
    this.lpDataHandler = lpDataHandler;
    this.lastFmDataHandler = lastFmDataHandler;
    this.singleTaskWaitingExecutor = new SingleTaskWaitingExecutor();
    this.started = false;
  }

  //////////////////
  // Bot Control

  public void start(TextChannel channel) {
    if (!started) {
      try {
        this.started = true;
        this.channel = channel;
        this.lpEntities = lpDataHandler.getLPEntityList();

        queueCountdown();
        queueSongs(this.lpEntities);
        singleTaskWaitingExecutor.start();
      } catch (Exception e) {
        this.started = false;
        channel.sendMessage("**Failed to start Listening Party due to an internal error!**");
        e.printStackTrace();
      }
    } else {
      channel.sendMessage("**A Listening Party is already in progress!**");
    }
  }

  public void stop(TextChannel channel) {
    if (started) {
      this.started = false;
      singleTaskWaitingExecutor.stop();
      this.channel.sendMessage("**Listening Party cancelled!**");
    } else {
      this.channel.sendMessage("**No active Listening Party.**");
    }
  }

  public void pause(TextChannel channel) {
    // TODO pause
  }

  public void resume(TextChannel channel) {
    // TODO resume
  }

  public String getLinkForChannel(TextChannel channel) {
    // TODO link
    return "";
  }

  //////////////////
  // Queue

  private void queueCountdown() {
    channel.sendMessage("**The Listening Party begins in...**");

    int COUNTDOWN_SECONDS = 10;
    for (int i = COUNTDOWN_SECONDS; i >= 0; i--) {
      String message = i > 0
          ? String.valueOf(i)
          : "\uD83C\uDF89 NOW \uD83C\uDF8A";
      int COUNTDOWN_INTERVAL_MS = 1000;
      singleTaskWaitingExecutor.schedule(() -> channel.sendMessage("**" + message + "**"), COUNTDOWN_INTERVAL_MS);
    }
  }

  private void queueSongs(List<LPEntity> lpEntityList) {
    List<String> songIds = lpEntityList.stream()
        .map(LPEntity::getSongId)
        .collect(Collectors.toList());
    Queue<LPEntity> lpEntityQueue = new LinkedList<>(lpEntityList);
    for (List<String> partition : Iterables.partition(songIds, 50)) {
      Track[] tracksPartition = SpotifyCall.execute(spotifyApi.getSeveralTracks(partition.toArray(String[]::new)));
      for (Track track : tracksPartition) {
        LPEntity lpEntity = lpEntityQueue.poll();
        if (lpEntity != null) {
          singleTaskWaitingExecutor.schedule(() -> {
            LPEntityWithLastFmData lastFmDataForLPEntity = lastFmDataHandler.getLastFmDataForLPEntity(lpEntity, track);
            sendDiscordEmbedToChannel(track, lastFmDataForLPEntity);
          }, track.getDurationMs());
        }
      }
    }
    singleTaskWaitingExecutor.schedule(() -> channel.sendMessage("**This Listening Party is over. Thank you for joining!**"), 0);
  }

  //////////////////
  // Discord

  private void sendDiscordEmbedToChannel(Track track, LPEntityWithLastFmData lpEntityWithLastFmData) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // "Subbed by: [entered name]
    // -> link to last.fm profile
    // -> with last.fm pfp
    String subbedBy = lpEntityWithLastFmData.getName();
    String lfmProfileLink = lpEntityWithLastFmData.getUserPageUrl();
    String lfmProfilePicture = lpEntityWithLastFmData.getProfilePictureUrl();
    String authorText = String.format("Submitted by: %s", subbedBy);
    if (lfmProfileLink != null && lfmProfilePicture != null) {
      embed.setAuthor(authorText, lfmProfileLink, lfmProfilePicture);
    } else {
      embed.setAuthor(authorText);
    }

    // "[Artist] – [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s – %s", songArtists, songTitle));

    String songLfmLink = lpEntityWithLastFmData.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Write-up
    if (!lpEntityWithLastFmData.getWriteUp().isBlank()) {
      String writeUp = Arrays.stream(lpEntityWithLastFmData.getWriteUp().split("<;;;>"))
          .map(s -> "> " + s)
          .collect(Collectors.joining("\n"));
      embed.setDescription("**Write-up:**\n" + writeUp);
    }

    // Field info
    String songOrdinal = lpEntityWithLastFmData.getOrdinal() + " / " + lpEntities.size();
    embed.addField("Party Entry:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    Integer scrobbleCount = lpEntityWithLastFmData.getScrobbleCount();
    if (scrobbleCount != null && scrobbleCount > 0) {
      embed.addField("Total Scrobbles:", String.valueOf(scrobbleCount), true);
    }

    // Full-res cover art
    String imageUrl = track.getAlbum().getImages()[0].getUrl();
    embed.setImage(imageUrl);

    // "Album: [Artist] – [Album] ([Release year])
    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = BotUtils.findReleaseYear(track);
    embed.setFooter(String.format("%s – %s\n(%s)", albumArtists, albumName, albumReleaseYear));

    // Add some finishing touches
    embed.setColor(EMBED_COLOR);

    // Send off the embed to the Discord channel
    channel.sendMessage(embed);
  }

  public boolean setLink(String potentialPlaylist) {
    return false;
  }
}
