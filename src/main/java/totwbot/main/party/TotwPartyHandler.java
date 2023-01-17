package totwbot.main.party;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;
import totwbot.main.lastfm.LastFmDataHandler;
import totwbot.main.lastfm.LastFmTotwData;
import totwbot.main.playlist.TotwDataHandler;
import totwbot.main.playlist.TotwEntity;
import totwbot.main.spotify.api.SpotifyCall;
import totwbot.main.spotify.util.BotUtils;

@Component
public class TotwPartyHandler {

  private final int COUNTDOWN_INTERVAL_MS = 1500;
  private final int COUNTDOWN_SECONDS = 1;

  private final Color EMBED_COLOR = new Color(173, 20, 87);

  private boolean started = false;
  private TextChannel channel;

  private final ScheduledExecutorService threadPool;
  private final Queue<TotwEntity> totwQueue;

  @Autowired
  private SpotifyApi spotifyApi;

  @Autowired
  private TotwDataHandler totwDataHandler;

  @Autowired
  private LastFmDataHandler lastFmDataHandler;

  @Autowired
  private ApplicationEventPublisher applicationEventPublisher;

  public TotwPartyHandler() {
    this.threadPool = new ScheduledThreadPoolExecutor(2);
    this.totwQueue = new LinkedList<>();
  }

  public boolean isStarted() {
    return started;
  }

  public void start(TextChannel channel) {
    if (!started) {
      this.started = true;
      this.channel = channel;
      startCountdown();
    } else {
      channel.sendMessage("**A TOTW party is already in progress!**");
    }
  }

  public void stop() {
    if (started) {
      // todo stop stuff
      this.started = false;
    } else {
      channel.sendMessage("**There's no TOTW to stop.**");
    }
  }

  //////////////////

  private void startCountdown() {
    channel.sendMessage("**The TOTW party begins in...**");
    Runnable countdown = createCountdown();
    threadPool.schedule(countdown, 1, TimeUnit.SECONDS);
  }

  private Runnable createCountdown() {
    return () -> {
      for (int i = COUNTDOWN_SECONDS; i > 0; i--) {
        channel.sendMessage("**" + i + "**");
        try {
          Thread.sleep(COUNTDOWN_INTERVAL_MS);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      channel.sendMessage("**\uD83C\uDF89 NOW \uD83C\uDF8A**");
      applicationEventPublisher.publishEvent(new CountdownFinishedEvent(this));
    };
  }

  //////////////////

  @EventListener(CountdownFinishedEvent.class)
  public void startMainParty() {
    totwQueue.addAll(totwDataHandler.getTotwEntityList());

    TotwEntity startingTrack = totwQueue.poll();
    if (startingTrack != null) {
      Runnable song = createRunnableForSong(startingTrack);
      threadPool.schedule(song, 1, TimeUnit.SECONDS);
    }
  }

  private Runnable createRunnableForSong(TotwEntity totwEntity) {
    return () -> {
      Track track = sendDiscordEmbedToChannel(totwEntity);

      // Prepare the next song in the queue
      TotwEntity nextSong = totwQueue.poll();
      Runnable nextSongRunnable;
      if (nextSong != null) {
        nextSongRunnable = createRunnableForSong(nextSong);
      } else {
        nextSongRunnable = () -> {
          channel.sendMessage("**The TOTW party is over. Thanks for joining!**");
          this.started = false;
        };
      }
//      threadPool.schedule(nextSongRunnable, track.getDurationMs(), TimeUnit.MILLISECONDS);
      threadPool.schedule(nextSongRunnable, 2, TimeUnit.SECONDS);
    };
  }

  private Track sendDiscordEmbedToChannel(TotwEntity totwEntity) {
    // Fetch the song information from Spotify's API
    Track track = SpotifyCall.execute(spotifyApi.getTrack(totwEntity.getSongId()));

    // Fetch the subber's last.fm additional data required to display everything properly
    LastFmTotwData lastFmDataForTotw = lastFmDataHandler.getLastFmDataForTotw(totwEntity.getLastFmName(), track);

    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // "Subbed by: [entered name]
    // -> link to last.fm profile
    // -> with last.fm pfp
    String subbedBy = totwEntity.getName();
    String lfmProfileLink = lastFmDataForTotw.getUserPageUrl();
    String lfmProfilePicture = lastFmDataForTotw.getProfilePictureUrl();
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

    String songLfmLink = lastFmDataForTotw.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Write-up
    String writeUp = Arrays.stream(totwEntity.getWriteUp().split("<;;;>"))
        .map(s -> "> " + s)
        .collect(Collectors.joining("\n"));
    embed.setDescription("**Write-up:**\n" + writeUp);

    // Field info
    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song length:", songLength, true);

    Integer scrobbleCount = lastFmDataForTotw.getScrobbleCount();
    if (scrobbleCount != null && scrobbleCount > 0) {
      embed.addField("Total scrobbles:", String.valueOf(scrobbleCount), true);
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
    return track;
  }

  public static class CountdownFinishedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    public CountdownFinishedEvent(Object source) {
      super(source);
    }
  }
}
