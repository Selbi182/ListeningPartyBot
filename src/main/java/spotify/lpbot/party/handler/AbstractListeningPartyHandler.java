package spotify.lpbot.party.handler;

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

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.DiscordUtils;
import spotify.lpbot.party.data.LPQueueEntity;
import spotify.lpbot.party.data.LPTarget;
import spotify.util.BotUtils;

public abstract class AbstractListeningPartyHandler {
  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<Long, ScheduledFuture<?>> nextFutures;
  private final Map<Long, CurrentTrack> currentTracks;

  AbstractListeningPartyHandler() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.nextFutures = new ConcurrentHashMap<>();
    this.currentTracks = new ConcurrentHashMap<>();
  }

  //////////////////
  // Bot Control

  public void start(TextChannel textChannel, LPTarget target) {
    try {
      if (!isStarted(textChannel)) {
        Queue<LPQueueEntity> eventQueue = new ConcurrentLinkedQueue<>(createTrackRunnables(textChannel, target));
        eventQueue.add(createFinalMessage(textChannel));
        recursiveSchedule(eventQueue, textChannel);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public EmbedBuilder stop(TextChannel textChannel) {
    if (isStarted(textChannel)) {
      nextFutures.remove(textChannel.getId()).cancel(true);
      currentTracks.remove(textChannel.getId());
      return DiscordUtils.createSimpleEmbed("Listening Party cancelled!");
    } else {
      return DiscordUtils.createErrorEmbed("No active Listening Party");
    }
  }

  public EmbedBuilder createStatusEmbed(TextChannel textChannel) {
    if (isStarted(textChannel)) {
      long delay = nextFutures.get(textChannel.getId()).getDelay(TimeUnit.MILLISECONDS);
      CurrentTrack currentTrack = currentTracks.get(textChannel.getId());
      if (currentTrack != null) {
        // todo album name
        EmbedBuilder embedBuilder = new EmbedBuilder();
        String message = String.format("%s – %s", BotUtils.getFirstArtistName(currentTrack.getTrack()), currentTrack.getTrack().getName());
        embedBuilder.setTitle(message);
        embedBuilder.addField("Track Number:", currentTrack.getTrackNumber() + " of " + currentTrack.getTotalTrackCount(), true);
        long passedTime = currentTrack.getTrack().getDurationMs() - delay;
        embedBuilder.addField("Timestamp:", BotUtils.formatTime(passedTime) + " / " + BotUtils.formatTime(currentTrack.getTrack().getDurationMs()), true);
        embedBuilder.setThumbnail(currentTrack.getImageUrl());
        return embedBuilder;
      } else {
        return DiscordUtils.createErrorEmbed("Wait for the countdown to finish");
      }
    } else {
      return DiscordUtils.createErrorEmbed("No active Listening Party");
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
      ScheduledFuture<?> future = this.scheduledExecutorService.schedule(() -> recursiveSchedule(eventQueue, textChannel), poll.getNextDelay(), TimeUnit.MILLISECONDS);
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

  private LPQueueEntity createFinalMessage(TextChannel textChannel) {
    return LPQueueEntity.of(() -> {
      DiscordUtils.sendMessage(textChannel, "\uD83C\uDF89 This Listening Party is over. Thank you for joining! \uD83C\uDF8A");
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
