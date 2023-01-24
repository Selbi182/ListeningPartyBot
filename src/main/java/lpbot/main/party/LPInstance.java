package lpbot.main.party;

import org.javacord.api.entity.channel.TextChannel;

public class LPInstance {

  private final TextChannel textChannel;
  private final LPTarget target;
  private final LPHandler lpHandler;

  public LPInstance(TextChannel textChannel, LPTarget target, LPHandler lpHandler) {
    this.textChannel = textChannel;
    this.target = target;
    this.lpHandler = lpHandler;
  }

  public String getAlbumOrPlaylistUri() {
    return target.getUri();
  }

  public boolean isStarted() {
    return lpHandler.isStarted(textChannel);
  }

  ///////////////////////

  public void start(int countdown) {
    if (!lpHandler.isStarted(textChannel)) {
      lpHandler.start(textChannel, target, countdown);
    }
  }

  public void stop() {
    if (lpHandler.isStarted(textChannel)) {
      lpHandler.stop(textChannel);
    }
  }
}
