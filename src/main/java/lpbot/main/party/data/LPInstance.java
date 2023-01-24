package lpbot.main.party.data;

import org.javacord.api.entity.channel.TextChannel;

import lpbot.main.party.AbstractListeningPartyHandler;

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

  public void start(int countdown) {
    lpHandler.start(textChannel, target, countdown);
  }

  public void stop() {
    lpHandler.stop(textChannel);
  }

  public void status() {
    lpHandler.printStatus(textChannel);
  }
}
