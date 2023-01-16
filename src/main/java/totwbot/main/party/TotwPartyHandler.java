package totwbot.main.party;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.Track;

import totwbot.main.playlist.TotwDataHandler;
import totwbot.main.playlist.TotwEntity;
import totwbot.spotify.api.SpotifyCall;
import totwbot.spotify.util.BotUtils;

@Component
public class TotwPartyHandler {

  private final int COUNTDOWN_INTERVAL_MS = 1500;
  private final int COUNTDOWN_SECONDS = 8;

  private boolean started = false;
  private TextChannel channel;

  private final ScheduledExecutorService threadPool;
  private final Queue<TotwEntity> totwQueue;

  @Autowired
  private SpotifyApi spotifyApi;

  @Autowired
  private TotwDataHandler totwDataHandler;

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
      Track track = SpotifyCall.execute(spotifyApi.getTrack(totwEntity.getSongId()));

      // TODO make the embed nicer. see https://discordjs.guide/popular-topics/embeds.html#using-the-embed-constructor
      EmbedBuilder replyEmbed = new EmbedBuilder();
      replyEmbed.setTitle(totwEntity.getName());
      replyEmbed.setDescription(totwEntity.getWriteUp().replace(";", "\n"));
      replyEmbed.setImage(track.getAlbum().getImages()[0].getUrl());
      replyEmbed.setFooter(BotUtils.joinArtists(track.getArtists()) + " – " + track.getName());
      replyEmbed.setColor(new Color(173, 20, 87));
      channel.sendMessage(replyEmbed);

      TotwEntity nextSong = totwQueue.poll();
      Runnable nextSongRunnable;
      if (nextSong != null) {
        nextSongRunnable = createRunnableForSong(nextSong);
      } else {
        nextSongRunnable = () -> channel.sendMessage("**The TOTW party is over. Thanks for joining!**");
      }
      threadPool.schedule(nextSongRunnable, track.getDurationMs(), TimeUnit.MILLISECONDS);
    };
  }

  public static class CountdownFinishedEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    public CountdownFinishedEvent(Object source) {
      super(source);
    }
  }
}
