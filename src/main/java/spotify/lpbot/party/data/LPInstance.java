package spotify.lpbot.party.data;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

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

  ///////////////////////

  public void start(InteractionImmediateResponseBuilder responder, int countdown) {
    lpHandler.start(responder, textChannel, target, countdown);
  }

  public void stop(InteractionImmediateResponseBuilder responder) {
    lpHandler.stop(responder, textChannel);
  }

  public void status(InteractionImmediateResponseBuilder responder) {
    lpHandler.printStatus(responder, textChannel);
  }
}
