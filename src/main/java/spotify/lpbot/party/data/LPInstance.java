package spotify.lpbot.party.data;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import spotify.lpbot.party.handler.AbstractListeningPartyHandler;

public class LPInstance {

  private final TextChannel textChannel;
  private final LPTarget target;
  private final AbstractListeningPartyHandler lpHandler;

  public LPInstance(TextChannel textChannel, LPTarget target, AbstractListeningPartyHandler lpHandler) {
    this.textChannel = textChannel;
    this.target = target;
    this.lpHandler = lpHandler;
  }

  public String getAlbumOrPlaylistUri() {
    return target.getLink();
  }

  public boolean isIdle() {
    return !lpHandler.isStarted(textChannel);
  }

  public TextChannel getTextChannel() {
    return textChannel;
  }

  ///////////////////////

  public void start() {
    lpHandler.start(textChannel, target);
  }

  public EmbedBuilder stop() {
    return lpHandler.stop(textChannel);
  }

  public EmbedBuilder status() {
    return lpHandler.createStatusEmbed(textChannel);
  }

}
