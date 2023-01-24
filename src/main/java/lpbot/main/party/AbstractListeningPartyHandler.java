package lpbot.main.party;

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

import lpbot.main.party.data.LPQueueEntity;
import lpbot.main.party.data.LPTarget;
import lpbot.main.spotify.util.BotUtils;
import se.michaelthelin.spotify.model_objects.specification.Track;

public abstract class AbstractListeningPartyHandler {
  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<Long, ScheduledFuture<?>> nextFutures;
  private final Map<Long, CurrentTrack> currentTracks;

  public AbstractListeningPartyHandler() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.nextFutures = new ConcurrentHashMap<>();
    this.currentTracks = new ConcurrentHashMap<>();
  }

  //////////////////
  // Bot Control

  public void start(TextChannel textChannel, LPTarget target, int countdown) {
    try {
      if (!isStarted(textChannel)) {
        Queue<LPQueueEntity> eventQueue = new ConcurrentLinkedQueue<>();
        eventQueue.addAll(createCountdownRunnables(textChannel, countdown));
        eventQueue.addAll(createTrackRunnables(textChannel, target));
        eventQueue.add(createFinalMessage(textChannel));
        recursiveSchedule(eventQueue, textChannel);
      } else {
        BotUtils.sendMessage(textChannel, "Listening Party is already in progress");
      }
    } catch (Exception e) {
      BotUtils.sendMessage(textChannel, "Failed to start Listening Party due to an internal error!");
      e.printStackTrace();
    }
  }

  public void stop(TextChannel textChannel) {
    if (isStarted(textChannel)) {
      nextFutures.remove(textChannel.getId()).cancel(true);
      currentTracks.remove(textChannel.getId());
    } else {
      BotUtils.sendMessage(textChannel, "No active Listening Party");
    }
  }

  public void printStatus(TextChannel textChannel) {
    if (isStarted(textChannel)) {
      long delay = nextFutures.get(textChannel.getId()).getDelay(TimeUnit.MILLISECONDS);
      CurrentTrack currentTrack = currentTracks.get(textChannel.getId());

      EmbedBuilder embedBuilder = new EmbedBuilder();
      embedBuilder.setTitle("Current Track");
      String message = String.format("%s – %s", BotUtils.getFirstArtistName(currentTrack.getTrack()), currentTrack.getTrack().getName());
      embedBuilder.setDescription(message);
      embedBuilder.addField("Track Number:", currentTrack.getTrackNumber() + " of " + currentTrack.getTotalTrackCount(), true);
      long passedTime = currentTrack.getTrack().getDurationMs() - delay;
      embedBuilder.addField("Timestamp:", BotUtils.formatTime(passedTime) + " / " + BotUtils.formatTime(currentTrack.getTrack().getDurationMs()), true);
      embedBuilder.setThumbnail(currentTrack.getImageUrl());

      textChannel.sendMessage(embedBuilder);

    } else {
      BotUtils.sendMessage(textChannel, "No active Listening Party");
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

  private List<LPQueueEntity> createTrackRunnables(TextChannel textChannel, LPTarget target) {
    List<Track> tracks = target.getTracks();
    List<LPQueueEntity> songRunnables = new ArrayList<>();
    for (int i = 0; i < tracks.size(); i++) {
      Track t = tracks.get(i);
      LPQueueEntity songRunnable = LPQueueEntity.of(createTrackRunnable(textChannel, target, i), t.getDurationMs());
      songRunnables.add(songRunnable);
    }
    return songRunnables;
  }


  private List<LPQueueEntity> createCountdownRunnables(TextChannel textChannel, int countdown) {
    int COUNTDOWN_INTERVAL_MS = 1000;
    List<LPQueueEntity> runnables = new ArrayList<>();
    LPQueueEntity firstMessage = LPQueueEntity.of(() -> BotUtils.sendMessage(textChannel, "The Listening Party begins in..."), COUNTDOWN_INTERVAL_MS);
    runnables.add(firstMessage);

    for (int i = countdown; i >= 0; i--) {
      String message = i > 0 ? String.valueOf(i) : "\uD83C\uDF89 NOW \uD83C\uDF8A";
      LPQueueEntity countdownMessage = LPQueueEntity.of(() -> BotUtils.sendMessage(textChannel, message), COUNTDOWN_INTERVAL_MS);
      runnables.add(countdownMessage);
    }

    return runnables;
  }

  private LPQueueEntity createFinalMessage(TextChannel textChannel) {
    return LPQueueEntity.of(() -> {
      BotUtils.sendMessage(textChannel, "\uD83C\uDF89 This Listening Party is over. Thank you for joining! \uD83C\uDF8A");
      nextFutures.remove(textChannel.getId());
      currentTracks.remove(textChannel.getId());
    }, 0);
  }

  //////////////////
  // Abstract

  protected abstract Runnable createTrackRunnable(TextChannel textChannel, LPTarget target, int trackNumber);

  protected void putCurrentTrack(long id, CurrentTrack currentTrack) {
    this.currentTracks.put(id, currentTrack);
  }

  //////////////////
  // Discord

  public static class CurrentTrack {
    private final Track track;
    private final int trackNumber;
    private final int totalTrackCount;
    private final String imageUrl;

    public CurrentTrack(Track track, int trackNumber, int totalTrackCount, String imageUrl) {
      this.track = track;
      this.trackNumber = trackNumber;
      this.totalTrackCount = totalTrackCount;
      this.imageUrl = imageUrl;
    }

    public Track getTrack() {
      return track;
    }

    public int getTrackNumber() {
      return trackNumber;
    }

    public int getTotalTrackCount() {
      return totalTrackCount;
    }

    public String getImageUrl() {
      return imageUrl;
    }

    @Override
    public String toString() {
      return String.format("(%d of %d) %s – %s", trackNumber, totalTrackCount, BotUtils.getFirstArtistName(track), track.getName());
    }
  }
}
