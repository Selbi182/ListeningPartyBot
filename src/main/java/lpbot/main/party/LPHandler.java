package lpbot.main.party;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Component;

import lpbot.main.spotify.util.BotUtils;
import se.michaelthelin.spotify.model_objects.specification.Track;

@Component
public class LPHandler {
  private final Color EMBED_COLOR = new Color(173, 20, 87);

  private final ScheduledExecutorService scheduledExecutorService;

  private final Map<Long, ScheduledFuture<?>> nextFutures;

  public LPHandler() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.nextFutures = new ConcurrentHashMap<>();
  }

  //////////////////
  // Bot Control

  public void start(TextChannel textChannel, LPTarget target, int countdown) {
      try {
        if (!isStarted(textChannel)) {
          Queue<LPQueueEntity> eventQueue = new ConcurrentLinkedQueue<>();
          eventQueue.addAll(createCountdownRunnables(textChannel, countdown));
          eventQueue.addAll(createTrackRunnables(textChannel, target.getTracks()));
          eventQueue.add(createFinalMessage(textChannel));
          recursiveSchedule(eventQueue, textChannel);
        }
      } catch (Exception e) {
        textChannel.sendMessage("**Failed to start Listening Party due to an internal error!**");
        e.printStackTrace();
      }
  }

  public void startTotw() {
    // TODO totw support
  }

  public void stop(TextChannel textChannel) {
    if (isStarted(textChannel)) {
      nextFutures.remove(textChannel.getId()).cancel(true);
    }
  }

  public boolean isStarted(TextChannel textChannel) {
    return nextFutures.containsKey(textChannel.getId());
  }

  //////////////////
  // Scheduling

  private void recursiveSchedule(Queue<LPQueueEntity> eventQueue, TextChannel textChannel) {
    LPQueueEntity poll = eventQueue.poll();
    if (poll != null) {
      this.scheduledExecutorService.execute(poll.getRunnable());
      ScheduledFuture<?> future =
          this.scheduledExecutorService.schedule(() -> recursiveSchedule(eventQueue, textChannel), poll.getNextDelay(),
              TimeUnit.MILLISECONDS);
      nextFutures.put(textChannel.getId(), future);
    }
  }


  private List<LPQueueEntity> createCountdownRunnables(TextChannel textChannel, int countdown) {
    int COUNTDOWN_INTERVAL_MS = 1000;
    List<LPQueueEntity> runnables = new ArrayList<>();
    LPQueueEntity firstMessage =
        LPQueueEntity.of(() -> textChannel.sendMessage("**The Listening Party begins in...**"), COUNTDOWN_INTERVAL_MS);
    runnables.add(firstMessage);

    for (int i = countdown; i >= 0; i--) {
      String message = i > 0
          ? String.valueOf(i)
          : "\uD83C\uDF89 NOW \uD83C\uDF8A";
      LPQueueEntity countdownMessage =
          LPQueueEntity.of(() -> textChannel.sendMessage("**" + message + "**"), COUNTDOWN_INTERVAL_MS);
      runnables.add(countdownMessage);
    }

    return runnables;
  }

  private List<LPQueueEntity> createTrackRunnables(TextChannel textChannel, List<Track> tracks) {
    List<LPQueueEntity> songRunnables = new ArrayList<>();
    for (int i = 0; i < tracks.size(); i++) {
      Track t = tracks.get(i);
      int trackNumber = i + 1;
      LPQueueEntity songRunnable =
          LPQueueEntity.of(() -> {
            EmbedBuilder discordEmbedForTrack = createDiscordEmbedForTrack(t, trackNumber, tracks.size());
            textChannel.sendMessage(discordEmbedForTrack);
          }, t.getDurationMs());
      songRunnables.add(songRunnable);
    }
    return songRunnables;
  }

  private LPQueueEntity createFinalMessage(TextChannel textChannel) {
    return LPQueueEntity.of(() -> {
      textChannel.sendMessage("\uD83C\uDF89 This Listening Party is over. Thank you for joining! \uD83C\uDF8A");
      nextFutures.remove(textChannel.getId());
    }, 0);
  }

//  public void start(TextChannel channel) {
//    if (!started) {
//      try {
//        this.started = true;
//        this.channel = channel;
//        this.lpEntities = lpDataHandler.getLPEntityList();
//
//        queueCountdown();
//        queueSongs(this.lpEntities);
//        singleTaskWaitingExecutor.start();
//      } catch (Exception e) {
//        this.started = false;
//        channel.sendMessage("**Failed to start Listening Party due to an internal error!**");
//        e.printStackTrace();
//      }
//    } else {
//      channel.sendMessage("**A Listening Party is already in progress!**");
//    }
//  }
//
//  public void stop(TextChannel channel) {
//    if (started) {
//      this.started = false;
//      singleTaskWaitingExecutor.stop();
//      this.channel.sendMessage("**Listening Party cancelled!**");
//    } else {
//      this.channel.sendMessage("**No active Listening Party.**");
//    }
//  }

  //////////////////
  // Queue

//  private void queueCountdown() {
//    channel.sendMessage("**The Listening Party begins in...**");
//
//    int COUNTDOWN_SECONDS = 10;
//    for (int i = COUNTDOWN_SECONDS; i >= 0; i--) {
//      String message = i > 0
//          ? String.valueOf(i)
//          : "\uD83C\uDF89 NOW \uD83C\uDF8A";
//      int COUNTDOWN_INTERVAL_MS = 1000;
//      singleTaskWaitingExecutor.schedule(() -> channel.sendMessage("**" + message + "**"), COUNTDOWN_INTERVAL_MS);
//    }
//  }
//
//  private void queueSongs(List<LPEntity> lpEntityList) {
//    List<String> songIds = lpEntityList.stream()
//        .map(LPEntity::getSongId)
//        .collect(Collectors.toList());
//    Queue<LPEntity> lpEntityQueue = new LinkedList<>(lpEntityList);
//    for (List<String> partition : Iterables.partition(songIds, 50)) {
//      Track[] tracksPartition = SpotifyCall.execute(spotifyApi.getSeveralTracks(partition.toArray(String[]::new)));
//      for (Track track : tracksPartition) {
//        LPEntity lpEntity = lpEntityQueue.poll();
//        if (lpEntity != null) {
//          singleTaskWaitingExecutor.schedule(() -> {
//            LPEntityWithLastFmData lastFmDataForLPEntity = lastFmDataHandler.getLastFmDataForLPEntity(lpEntity, track);
//            sendDiscordEmbedToChannel(track, lastFmDataForLPEntity);
//          }, track.getDurationMs());
//        }
//      }
//    }
//    singleTaskWaitingExecutor.schedule(() -> channel.sendMessage("**This Listening Party is over. Thank you for joining!**"), 0);
//  }

  //////////////////
  // Discord


  private EmbedBuilder createDiscordEmbedForTrack(Track track, int trackNumber, int totalTrackCount) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // "[Artist] – [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s – %s", songArtists, songTitle));

    String songLfmLink = track.getExternalUrls().get("spotify");
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Field info
    String songOrdinal = trackNumber + " / " + totalTrackCount;
    embed.addField("Track Number:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

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

    return embed;
  }


//  private void sendDiscordEmbedToChannel(Track track, LPEntityWithLastFmData lpEntityWithLastFmData) {
//    // Prepare a new Discord embed
//    EmbedBuilder embed = new EmbedBuilder();
//
//    // "Subbed by: [entered name]
//    // -> link to last.fm profile
//    // -> with last.fm pfp
//    String subbedBy = lpEntityWithLastFmData.getName();
//    String lfmProfileLink = lpEntityWithLastFmData.getUserPageUrl();
//    String lfmProfilePicture = lpEntityWithLastFmData.getProfilePictureUrl();
//    String authorText = String.format("Submitted by: %s", subbedBy);
//    if (lfmProfileLink != null && lfmProfilePicture != null) {
//      embed.setAuthor(authorText, lfmProfileLink, lfmProfilePicture);
//    } else {
//      embed.setAuthor(authorText);
//    }
//
//    // "[Artist] – [Title] ([song:length])
//    // -> Link to last.fm page
//    String songArtists = BotUtils.joinArtists(track.getArtists());
//    String songTitle = track.getName();
//    embed.setTitle(String.format("%s – %s", songArtists, songTitle));
//
//    String songLfmLink = lpEntityWithLastFmData.getSongLinkUrl();
//    if (songLfmLink != null) {
//      embed.setUrl(songLfmLink);
//    }
//
//    // Write-up
//    if (!lpEntityWithLastFmData.getWriteUp().isBlank()) {
//      String writeUp = Arrays.stream(lpEntityWithLastFmData.getWriteUp().split("<;;;>"))
//          .map(s -> "> " + s)
//          .collect(Collectors.joining("\n"));
//      embed.setDescription("**Write-up:**\n" + writeUp);
//    }
//
//    // Field info
//    String songOrdinal = lpEntityWithLastFmData.getOrdinal() + " / " + lpEntities.size();
//    embed.addField("Party Entry:", songOrdinal, true);
//
//    String songLength = BotUtils.formatTime(track.getDurationMs());
//    embed.addField("Song Length:", songLength, true);
//
//    Integer scrobbleCount = lpEntityWithLastFmData.getScrobbleCount();
//    if (scrobbleCount != null && scrobbleCount > 0) {
//      embed.addField("Total Scrobbles:", String.valueOf(scrobbleCount), true);
//    }
//
//    // Full-res cover art
//    String imageUrl = track.getAlbum().getImages()[0].getUrl();
//    embed.setImage(imageUrl);
//
//    // "Album: [Artist] – [Album] ([Release year])
//    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
//    String albumName = track.getAlbum().getName();
//    String albumReleaseYear = BotUtils.findReleaseYear(track);
//    embed.setFooter(String.format("%s – %s\n(%s)", albumArtists, albumName, albumReleaseYear));
//
//    // Add some finishing touches
//    embed.setColor(EMBED_COLOR);
//
//    // Send off the embed to the Discord channel
//    channel.sendMessage(embed);
//  }
}
