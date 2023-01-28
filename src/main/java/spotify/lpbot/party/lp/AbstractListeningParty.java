package spotify.lpbot.party.lp;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.util.BotUtils;

public abstract class AbstractListeningParty {
  private static final long COUNTDOWN_INTERVAL_MS = 1000;

  private enum State {
    READY,
    COUNTDOWN,
    RESUME_COUNTDOWN,
    ONGOING,
    PAUSED
  }

  private final ScheduledExecutorService scheduledExecutorService;

  private final TextChannel channel;
  private final TrackListWrapper trackListWrapper;

  private State state;
  private int currentTrackListIndex;
  private ScheduledFuture<?> nextFuture;
  private Long remainingTimeAtTimeOfPause;

  public AbstractListeningParty(TextChannel channel, TrackListWrapper trackListWrapper) {
    this.channel = channel;
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.trackListWrapper = trackListWrapper;
    this.currentTrackListIndex = 0;
    this.state = State.READY;
  }

  public boolean isOverwritable() {
    return isState(State.READY);
  }

  public void start(InteractionOriginalResponseUpdater responder, int countdown) {
    if (isState(State.READY)) {
      EmbedBuilder countdownEmbed = DiscordUtils.createSimpleEmbed("The Listening Party begins in...", true);
      remainingTimeAtTimeOfPause = null;
      state = State.COUNTDOWN;
      createAndStartCountdown(responder, countdownEmbed, countdown, false);
    } else if (isState(State.PAUSED)) {
      EmbedBuilder resumeEmbed = DiscordUtils.createSimpleEmbed("Resuming Listening Party in...", true);
      state = State.RESUME_COUNTDOWN;
      createAndStartCountdown(responder, resumeEmbed, countdown, true);
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is already active");
    }
  }

  public void stop(InteractionOriginalResponseUpdater responder) {
    if (isState(State.COUNTDOWN, State.RESUME_COUNTDOWN, State.ONGOING, State.PAUSED)) {
      nextFuture.cancel(true);
      state = State.READY;
      currentTrackListIndex = 0;
      remainingTimeAtTimeOfPause = null;
      DiscordUtils.updateWithSimpleEmbed(responder, "Listening Party stopped!");
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is not active");
    }
  }

  public void next(InteractionOriginalResponseUpdater responder, int skipAmount) {
    int maxSkipAmount = getTotalTrackCount() - getCurrentTrackListIndex();
    int skipAmountLimited = Math.min(skipAmount, maxSkipAmount);
    if (isState(State.ONGOING)) {
      nextFuture.cancel(true);
      currentTrackListIndex += skipAmountLimited;
      printNextSong();
      DiscordUtils.updateWithSimpleEmbed(responder, skipAmountLimited + " song(s) skipped!");
    } else if (isState(State.COUNTDOWN, State.RESUME_COUNTDOWN)) {
      DiscordUtils.updateWithErrorEmbed(responder, "Countdowns can't be skipped");
    } else if (isState(State.PAUSED, State.READY)) {
      currentTrackListIndex += skipAmountLimited;
      remainingTimeAtTimeOfPause = null;
      nowPlaying(responder);
      DiscordUtils.updateWithSimpleEmbed(responder, skipAmountLimited + " song(s) skipped! (Note: Listening Party is still paused. Use `/start` to resume)");
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is not active");
    }
  }

  public void previous(InteractionOriginalResponseUpdater responder, int goBackAmount) {
    int goBackAmountLimited = Math.max(0, Math.min(goBackAmount, getCurrentTrackListIndex()));
    if (isState(State.ONGOING)) {
      nextFuture.cancel(true);
      currentTrackListIndex -= goBackAmountLimited;
      printNextSong();
      DiscordUtils.updateWithSimpleEmbed(responder, "Went back " + goBackAmountLimited + " song(s)!");
    } else if (isState(State.COUNTDOWN, State.RESUME_COUNTDOWN)) {
      DiscordUtils.updateWithErrorEmbed(responder, "There is nothing before the countdown");
    } else if (isState(State.PAUSED, State.READY)) {
      currentTrackListIndex -= goBackAmountLimited;
      remainingTimeAtTimeOfPause = null;
      nowPlaying(responder);
      DiscordUtils.updateWithSimpleEmbed(responder, "Went back " + goBackAmountLimited + " song(s)! (Note: Listening Party is still paused. Use `/start` to resume)");
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is not active");
    }
  }

  public void pause(InteractionOriginalResponseUpdater responder) {
    if (isState(State.ONGOING)) {
      nextFuture.cancel(true);
      remainingTimeAtTimeOfPause = nextFuture.getDelay(TimeUnit.MILLISECONDS);
      state = State.PAUSED;
      nowPlaying(responder);
    } else if (isState(State.PAUSED)) {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is already paused");
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is not active or can't be paused right now");
    }
  }

  public void restart(InteractionOriginalResponseUpdater responder) {
    if (isState(State.ONGOING)) {
      nextFuture.cancel(true);
      prepareNextSong(getCurrentTrack());
      nowPlaying(responder);
    } else if (isState(State.PAUSED)) {
      remainingTimeAtTimeOfPause = null;
      DiscordUtils.updateWithSimpleEmbed(responder, "Song restarted! (Note: Listening Party is still paused. Use `/start` to resume)");
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "Listening Party is not active or can't be interacted with right now");
    }
  }

  public void nowPlaying(InteractionOriginalResponseUpdater responder) {
    if (isState(State.ONGOING, State.PAUSED, State.READY)) {
      if (getCurrentTrackListIndex() < getTotalTrackCount()) {
        Track currentTrack = getAllTracks().get(Math.min(getCurrentTrackListIndex(), getTotalTrackCount() - 1));

        EmbedBuilder nowPlayingEmbed = new EmbedBuilder();

        nowPlayingEmbed.setTitle(String.format("%s \u2013 %s", BotUtils.getFirstArtistName(currentTrack), currentTrack.getName()));
        nowPlayingEmbed.setDescription("> From **" + currentTrack.getAlbum().getName() + "** (" + BotUtils.findReleaseYear(currentTrack) + ")");
        nowPlayingEmbed.setThumbnail(BotUtils.findLargestImage(currentTrack.getAlbum().getImages()));
        nowPlayingEmbed.setColor(getColorForCurrentTrack());

        nowPlayingEmbed.addField("Listening Party Link:", getTrackListWrapper().getLink());

        nowPlayingEmbed.addField("Track Number:", getCurrentTrackNumber() + " of " + getTotalTrackCount(), true);
        Integer songLength = currentTrack.getDurationMs();
        long passedTime;
        boolean paused = State.PAUSED.equals(state) || State.READY.equals(state);
        if (paused) {
          if (remainingTimeAtTimeOfPause != null) {
            passedTime = songLength - remainingTimeAtTimeOfPause;
          } else {
            passedTime = 0;
          }
        } else {
          passedTime = songLength - nextFuture.getDelay(TimeUnit.MILLISECONDS);
        }
        nowPlayingEmbed.addField("Timestamp:", BotUtils.formatTime(passedTime) + " / " + BotUtils.formatTime(songLength) + (paused ? " *(Paused)*" : ""), true);

//        if (!paused) {
//          // todo periodic updating of the timestamp (for a while, not sure how long yet)
//          nowPlayingEmbed.setFooter("(Timestamp will stop updating in 60 seconds!)");
//        }

        DiscordUtils.respondWithEmbed(responder, nowPlayingEmbed);
      } else {
        printFinalMessage();
      }
    } else if (isState(State.COUNTDOWN, State.RESUME_COUNTDOWN)) {
      DiscordUtils.updateWithErrorEmbed(responder, "Wait for the countdown to finish");
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "No active Listening Party");
    }
  }

  public void link(InteractionOriginalResponseUpdater responder) {
    // Must be a simple message instead of an embed, otherwise the Spotify preview won't work
    DiscordUtils.respondWithMessage(responder, getTrackListWrapper().getLink());
  }

  ////////////////////

  private void printNextSong() {
    if (getCurrentTrackListIndex() < getTotalTrackCount()) {
      // Send info about current song to channel
      Track currentTrack = getCurrentTrack();
      EmbedBuilder discordEmbedForTrack = createDiscordEmbedForTrack(currentTrack);
      channel.sendMessage(discordEmbedForTrack);

      // Prepare the next song
      prepareNextSong(currentTrack);
    } else {
      // This was the final song. End it
      printFinalMessage();
    }
  }

  private void prepareNextSong(Track currentTrack) {
    this.nextFuture = scheduledExecutorService.schedule(() -> {
      currentTrackListIndex++;
      printNextSong();
    }, currentTrack.getDurationMs(), TimeUnit.MILLISECONDS);
  }

  private void printFinalMessage() {
    currentTrackListIndex = 0;
    this.state = State.READY;
    DiscordUtils.sendSimpleEmbed(channel, "\uD83C\uDF89 This Listening Party is over. Thank you for joining! \uD83C\uDF8A");
  }

  private void createAndStartCountdown(InteractionOriginalResponseUpdater responder, EmbedBuilder countdownEmbed, int countdown, boolean resume) {
    Message message = DiscordUtils.respondWithEmbed(responder, countdownEmbed).join();
    AtomicInteger atomicCountdown = new AtomicInteger(countdown);
    this.nextFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
      int i = atomicCountdown.getAndDecrement();
      StringBuilder description = new StringBuilder();
      for (int j = countdown; j >= i; j--) {
        description.append(j > 0 ? j + "... " : "\uD83C\uDF89 NOW \uD83C\uDF8A");
      }
      countdownEmbed.setDescription(description.toString());
      message.createUpdater().addEmbed(countdownEmbed).replaceMessage().join();
      if (i <= 0) {
        this.state = State.ONGOING;
        this.nextFuture.cancel(true);
        if (resume) {
          long delayAfterPause;
          if (remainingTimeAtTimeOfPause == null) {
            delayAfterPause = getCurrentTrack().getDurationMs();
          } else {
            delayAfterPause = remainingTimeAtTimeOfPause;
          }
          this.nextFuture = scheduledExecutorService.schedule(this::printNextSong, delayAfterPause, TimeUnit.MILLISECONDS);
          nowPlaying(responder);
        } else {
          scheduledExecutorService.execute(this::printNextSong);
        }
      }
    }, COUNTDOWN_INTERVAL_MS, COUNTDOWN_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }

  ////////////////////
  // Util

  protected boolean isState(State... states) {
    return Arrays.stream(states).anyMatch(state -> Objects.equals(state, this.state));
  }

  protected int getCurrentTrackListIndex() {
    return currentTrackListIndex;
  }

  protected List<Track> getAllTracks() {
    return getTrackListWrapper().getTracks();
  }

  protected Track getCurrentTrack() {
    return getAllTracks().get(getCurrentTrackListIndex());
  }

  protected int getCurrentTrackNumber() {
    return getCurrentTrackListIndex() + 1;
  }

  protected int getTotalTrackCount() {
    return getAllTracks().size();
  }

  protected Color getColorForCurrentTrack() {
    return getTrackListWrapper().getColorByTrackIndex(getCurrentTrackListIndex());
  }

  protected TrackListWrapper getTrackListWrapper() {
    return trackListWrapper;
  }

  ////////////////////
  // Abstract Discord logic

  protected abstract EmbedBuilder createDiscordEmbedForTrack(Track track);
}
