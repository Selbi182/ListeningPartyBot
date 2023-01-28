package spotify.lpbot.party.lp;

import java.awt.Color;
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

public class StandardListeningParty {
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
  private final AtomicInteger currentTrackListIndex;

  private State state;
  private ScheduledFuture<?> nextFuture;
  private Long remainingTimeAtTimeOfPause;

  public StandardListeningParty(TextChannel channel, TrackListWrapper trackListWrapper) {
    this.channel = channel;
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.trackListWrapper = trackListWrapper;
    this.currentTrackListIndex = new AtomicInteger();
    this.state = State.READY;
  }

  public void start(InteractionOriginalResponseUpdater responder, int countdown) {
    if (State.READY.equals(state)) {
      EmbedBuilder countdownEmbed = DiscordUtils.createSimpleEmbed("The Listening Party begins in...", true);
      currentTrackListIndex.set(-1);
      remainingTimeAtTimeOfPause = null;
      state = State.COUNTDOWN;
      createAndStartCountdown(responder, countdownEmbed, countdown, false);
    } else if (state.equals(State.PAUSED)) {
      EmbedBuilder resumeEmbed = DiscordUtils.createSimpleEmbed("Resuming Listening Party in...", true);
      state = State.RESUME_COUNTDOWN;
      createAndStartCountdown(responder, resumeEmbed, countdown, true);
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party is already active")).update();
    }
  }

  public void stop(InteractionOriginalResponseUpdater responder) {
    if (State.COUNTDOWN.equals(state) || State.RESUME_COUNTDOWN.equals(state) || State.ONGOING.equals(state) || State.PAUSED.equals(state)) {
      if (nextFuture.cancel(true)) {
        state = State.READY;
        currentTrackListIndex.set(-1);
        remainingTimeAtTimeOfPause = null;
        responder.addEmbed(DiscordUtils.createSimpleEmbed("Listening Party stopped!")).update();
      } else {
        responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party could not be stopped due to an internal error")).update();
      }
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party is not active")).update();
    }
  }

  public void skip(InteractionOriginalResponseUpdater responder) {
    if (State.ONGOING.equals(state)) {
      if (nextFuture.cancel(true)) {
        scheduledExecutorService.execute(getSongRunnable());
        responder.addEmbed(DiscordUtils.createSimpleEmbed("Song skipped!")).update();
      } else {
        responder.addEmbed(DiscordUtils.createErrorEmbed("Song could not be skipped due to an internal error")).update();
      }
    } else if (State.COUNTDOWN.equals(state) || State.RESUME_COUNTDOWN.equals(state)) {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Countdowns can't be skipped")).update();
    } else if (State.PAUSED.equals(state)) {
      currentTrackListIndex.incrementAndGet();
      responder.addEmbed(DiscordUtils.createSimpleEmbed("Song skipped! (Note: Listening Party is still paused. Use `/start` to resume)")).update();
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party is not active")).update();
    }
  }

  public void pause(InteractionOriginalResponseUpdater responder) {
    if (State.ONGOING.equals(state)) {
      long delay = nextFuture.getDelay(TimeUnit.MILLISECONDS);
      if (nextFuture.cancel(true)) {
        remainingTimeAtTimeOfPause = delay;
        state = State.PAUSED;
        nowPlaying(responder);
      } else {
        responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party could not be paused due to an internal error")).update();
      }
    } else if (State.PAUSED.equals(state)) {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party is already paused")).update();
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Listening Party is not active or can't be paused right now")).update();
    }
  }

  public void nowPlaying(InteractionOriginalResponseUpdater responder) {
    if (State.ONGOING.equals(state) || State.PAUSED.equals(state)) {
      Track currentTrack = trackListWrapper.getTracks().get(currentTrackListIndex.get());

      EmbedBuilder nowPlayingEmbed = new EmbedBuilder();

      nowPlayingEmbed.setTitle(String.format("%s \u2013 %s", BotUtils.getFirstArtistName(currentTrack), currentTrack.getName()));
      nowPlayingEmbed.setDescription("> From **" + currentTrack.getAlbum().getName() + "** (" + BotUtils.findReleaseYear(currentTrack) + ")");
      nowPlayingEmbed.setThumbnail(BotUtils.findLargestImage(currentTrack.getAlbum().getImages()));
      nowPlayingEmbed.setColor(trackListWrapper.getColorForTrack(currentTrack));

      nowPlayingEmbed.addField("Listening Party Link:", trackListWrapper.getLink());

      nowPlayingEmbed.addField("Track Number:", getCurrentTrackNumber() + " of " + trackListWrapper.getTracks().size(), true);
      Integer songLength = currentTrack.getDurationMs();
      long passedTime;
      boolean paused = State.PAUSED.equals(state);
      if (paused) {
        passedTime = songLength - remainingTimeAtTimeOfPause;
      } else {
        passedTime = songLength - nextFuture.getDelay(TimeUnit.MILLISECONDS);
      }
      nowPlayingEmbed.addField("Timestamp:", BotUtils.formatTime(passedTime) + " / " + BotUtils.formatTime(songLength) + (paused ? " *(Paused)*" : ""), true);

      if (!paused) {
        nowPlayingEmbed.setFooter("(Timestamp will stop updating in 60 seconds!)");
      }

      responder.addEmbed(nowPlayingEmbed).update();

      // todo periodic updating of the timestamp (for a while, not sure how long yet)
    } else if (State.COUNTDOWN.equals(state) || State.RESUME_COUNTDOWN.equals(state)) {
      responder.addEmbed(DiscordUtils.createErrorEmbed("Wait for the countdown to finish")).update();
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("No active Listening Party")).update();
    }
  }

  public void link(InteractionOriginalResponseUpdater responder) {
    responder.setContent(trackListWrapper.getLink()).update();
  }

  ////////////////////

  private Runnable getSongRunnable() {
    return () -> {
      int songIndex = this.currentTrackListIndex.incrementAndGet();
      if (songIndex < trackListWrapper.getTracks().size()) {
        // Send info about current song to channel
        Track currentTrack = trackListWrapper.getTracks().get(songIndex);
        EmbedBuilder discordEmbedForTrack = createDiscordEmbedForTrack(currentTrack);
        channel.sendMessage(discordEmbedForTrack);

        // Prepare the next song
        this.nextFuture = scheduledExecutorService.schedule(this::getSongRunnable, currentTrack.getDurationMs(), TimeUnit.MILLISECONDS);
      } else {
        // This was the final song. End it
        DiscordUtils.sendSimpleEmbed(channel, "\uD83C\uDF89 This Listening Party is over. Thank you for joining! \uD83C\uDF8A");
        this.state = State.READY;
      }
    };
  }

  public void createAndStartCountdown(InteractionOriginalResponseUpdater responder, EmbedBuilder countdownEmbed, int countdown, boolean resume) {
    Message msg = responder.addEmbed(countdownEmbed).update().join();
    AtomicInteger atomicCountdown = new AtomicInteger(countdown);
    this.nextFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
      int i = atomicCountdown.getAndDecrement();
      StringBuilder description = new StringBuilder();
      for (int j = countdown; j >= i; j--) {
        description.append(j > 0 ? j + "... " : "\uD83C\uDF89 NOW \uD83C\uDF8A");
      }
      countdownEmbed.setDescription(description.toString());
      msg.createUpdater().addEmbed(countdownEmbed).replaceMessage().join();
      if (i <= 0) {
        this.state = State.ONGOING;
        this.nextFuture.cancel(true);
        if (resume) {
          this.nextFuture = scheduledExecutorService.schedule(getSongRunnable(), remainingTimeAtTimeOfPause, TimeUnit.MILLISECONDS);
          nowPlaying(responder);
        } else {
          scheduledExecutorService.execute(getSongRunnable());
        }
      }
    }, COUNTDOWN_INTERVAL_MS, COUNTDOWN_INTERVAL_MS, TimeUnit.MILLISECONDS);
  }


  public boolean isOverwritable() {
    return State.READY.equals(state);
  }

  private int getCurrentTrackNumber() {
    return currentTrackListIndex.get() + 1;
  }

  private EmbedBuilder createDiscordEmbedForTrack(Track track) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // "[Artist] - [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    String songLfmLink = track.getExternalUrls().get("spotify");
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Field info
    String songOrdinal = getCurrentTrackNumber() + " of " + trackListWrapper.getTracks().size();
    embed.addField("Track Number:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    // Image and color
    String imageUrl = BotUtils.findLargestImage(track.getAlbum().getImages());
    embed.setImage(imageUrl);

    Color embedColor = trackListWrapper.getColorForTrack(track);
    embed.setColor(embedColor);

    // "Album: [Artist] - [Album] ([Release year])
    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = BotUtils.findReleaseYear(track);
    embed.setFooter(String.format("%s \u2013 %s (%s)", albumArtists, albumName, albumReleaseYear));

    // Send off the embed to the Discord channel
    return embed;
  }
}
