package spotify.lpbot.party.lp;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.javacord.api.entity.Icon;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.enums.ModelObjectType;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.lpbot.party.lp.misc.FinalMessages;
import spotify.lpbot.party.lp.misc.LpUtils;
import spotify.util.SpotifyUtils;

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
  private final FinalMessages finalMessages;
  private final Logger logger;

  private State state;
  private int currentTrackListIndex;
  private ScheduledFuture<?> nextFuture;
  private Long remainingTimeAtTimeOfPause;


  public AbstractListeningParty(TextChannel channel, TrackListWrapper trackListWrapper, FinalMessages finalMessages) {
    this.channel = channel;
    this.trackListWrapper = trackListWrapper;
    this.finalMessages = finalMessages;
    this.logger = Logger.getLogger(AbstractListeningParty.class.getName());

    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.currentTrackListIndex = 0;
    this.state = State.READY;
  }

  public boolean isReplaceable() {
    return isState(State.READY);
  }

  public void start(InteractionOriginalResponseUpdater responder, int countdown) {
    if (isState(State.READY)) {
      EmbedBuilder countdownEmbed = DiscordUtils.createSimpleEmbed(String.format("The Listening Party begins%s...", countdown > 0 ? " in" : ""), true);
      remainingTimeAtTimeOfPause = null;
      state = State.COUNTDOWN;
      createAndStartCountdown(responder, countdownEmbed, countdown, false);
      logLpStart(channel);
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
      logLpEnd(channel, true);
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

        nowPlayingEmbed.setTitle(String.format("%s \u2013 %s", SpotifyUtils.getFirstArtistName(currentTrack), currentTrack.getName()));
        nowPlayingEmbed.setDescription("> From **" + currentTrack.getAlbum().getName() + "** (" + SpotifyUtils.findReleaseYear(currentTrack) + ")");
        nowPlayingEmbed.setThumbnail(SpotifyUtils.findLargestImage(currentTrack.getAlbum().getImages()));
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
        nowPlayingEmbed.addField("Timestamp:", SpotifyUtils.formatTime(passedTime) + " / " + SpotifyUtils.formatTime(songLength) + (paused ? " *(Paused)*" : ""), true);
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

    EmbedBuilder finalEmbed = DiscordUtils.createSimpleEmbed(finalMessages.getRandomFinalMessage(), false);
    attachServerName(finalEmbed);
    if (ModelObjectType.ALBUM.equals(trackListWrapper.getLpType())) {
      attachFooter(getCurrentTrack(), finalEmbed);
      finalEmbed.setColor(getColorForCurrentTrack());
    }
    channel.sendMessage(finalEmbed);

    logLpEnd(channel, false);
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
  // Attachment Utils

  /**
   * Attach the server name as author in the following format:
   * [Server Image] Listening Party on: [Server Name]
   */
  protected void attachServerName(EmbedBuilder embed) {
    Optional<ServerChannel> serverChannel = getChannel().asServerChannel();
    if (serverChannel.isPresent()) {
      Server server = serverChannel.get().getServer();
      String serverName = server.getName();
      Optional<Icon> serverIcon = server.getIcon();
      String lpTitle = "Listening Party on: " + serverName;
      if (serverIcon.isPresent()) {
        embed.setAuthor(lpTitle, null, serverIcon.get());
      } else {
        embed.setAuthor(lpTitle);
      }
    }
  }

  /**
   * Attach a footer of the current track's album in the following format:
   * [Release Type]: [Artist Name] - [Album Name] (Release Year)
   */
  protected void attachFooter(Track track, EmbedBuilder embed) {
    String albumArtists = SpotifyUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = SpotifyUtils.findReleaseYear(track);
    String releaseType = track.getAlbum().getAlbumType().toString();
    embed.setFooter(String.format("%s: %s \u2013 %s (%s)", releaseType, albumArtists, albumName, albumReleaseYear));
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
    ColorFetchResult.RGB rgb = getTrackListWrapper().getColorByTrackIndex(getCurrentTrackListIndex());
    return new Color(rgb.getR(), rgb.getG(), rgb.getB());
  }

  protected TrackListWrapper getTrackListWrapper() {
    return trackListWrapper;
  }

  protected TextChannel getChannel() {
    return channel;
  }

  ////////////////////
  // Log Utils

  private void logLpStart(TextChannel textChannel) {
    LpUtils.logLpEvent(textChannel, logger, "LP started");
  }

  private void logLpEnd(TextChannel textChannel, boolean aborted) {
    LpUtils.logLpEvent(textChannel, logger, "LP " + (aborted ? "aborted" : "concluded"));
  }

  ////////////////////
  // Abstract Discord logic

  protected abstract EmbedBuilder createDiscordEmbedForTrack(Track track);
}
