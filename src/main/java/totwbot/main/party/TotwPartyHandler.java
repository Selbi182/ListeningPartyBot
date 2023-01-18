package totwbot.main.party;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final int COUNTDOWN_SECONDS = 10;

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

  private final Set<ScheduledFuture<?>> futures;

  private Runnable nextRunnable;

  private int totalSongCount;

  public TotwPartyHandler() {
    this.threadPool = new ScheduledThreadPoolExecutor(2);
    this.totwQueue = new LinkedList<>();
    this.futures = new ConcurrentSkipListSet<>();
  }

  public boolean isStarted() {
    return started;
  }

  public void start(TextChannel channel) {
    if (!started) {
      try {
        this.started = true;
        this.channel = channel;
        totwQueue.clear();
        List<TotwEntity> totwEntityList = totwDataHandler.getTotwEntityList();
        totalSongCount = totwEntityList.size();
        totwQueue.addAll(totwEntityList);
        startCountdown();
      } catch (Exception e) {
        this.started = false;
        channel.sendMessage("**Failed to start TOTW due to an internal error!**");
        e.printStackTrace();
      }
    } else {
      channel.sendMessage("**A TOTW party is already in progress!**");
    }
  }

  public void stop() {
    if (started) {
      this.started = false;
      cancelAllTasks();
      channel.sendMessage("**TOTW session cancelled!**");
    } else {
      channel.sendMessage("**No active TOTW session.**");
    }
  }

  public void skip() {
    if (started && nextRunnable != null) {
      Runnable temp = nextRunnable;
      cancelAllTasks();
      scheduleRunnableImmediate(temp);
      channel.sendMessage("**Skipped!**");
    } else {
      channel.sendMessage("**No active TOTW session!**");
    }

  }

  //////////////////

  private void cancelAllTasks() {
    for (ScheduledFuture<?> future : futures) {
      future.cancel(true);
    }
    futures.clear();
    nextRunnable = null;
  }

  private void startCountdown() {
    channel.sendMessage("**The TOTW party begins in...**");
    Runnable countdown = createCountdown();
    scheduleRunnableImmediate(countdown);
  }

  private Runnable createCountdown() {
    return () -> {
      // Schedule first song
      scheduleNextSong(COUNTDOWN_SECONDS * COUNTDOWN_INTERVAL_MS, TimeUnit.MILLISECONDS);

      // Simulate countdown
      for (int i = COUNTDOWN_SECONDS; i > 0; i--) {
        channel.sendMessage("**" + i + "**");
        try {
          Thread.sleep(COUNTDOWN_INTERVAL_MS);
        } catch (InterruptedException e) {
          return;
        }
      }
      channel.sendMessage("**\uD83C\uDF89 NOW \uD83C\uDF8A**");
    };
  }


  private void scheduleRunnableImmediate(Runnable runnable) {
    scheduleRunnableImmediate(runnable, 0, TimeUnit.SECONDS);
  }

  private void scheduleRunnableImmediate(Runnable runnable, int delay, TimeUnit unit) {
    ScheduledFuture<?> future = threadPool.schedule(runnable, delay, unit);
    futures.add(future);
    nextRunnable = runnable;
  }

  //////////////////

  private void scheduleNextSong(int delay, TimeUnit unit) {
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
    scheduleRunnableImmediate(nextSongRunnable, delay, unit);
  }

  private Runnable createRunnableForSong(TotwEntity totwEntity) {
    return () -> {
      if (started) {
        Track track = sendDiscordEmbedToChannel(totwEntity);
        scheduleNextSong(track.getDurationMs(), TimeUnit.MILLISECONDS);
      }
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
    if (!totwEntity.getWriteUp().isBlank()) {
      String writeUp = Arrays.stream(totwEntity.getWriteUp().split("<;;;>"))
              .map(s -> "> " + s)
              .collect(Collectors.joining("\n"));
      embed.setDescription("**Write-up:**\n" + writeUp);
    }

    // Field info
    String songOrdinal = totwEntity.getOrdinal() + " / " + totalSongCount;
    embed.addField("TOTW Entry:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    Integer scrobbleCount = lastFmDataForTotw.getScrobbleCount();
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
    return track;
  }
}
