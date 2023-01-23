package lpbot.main.playlist;

import java.net.MalformedURLException;
import java.net.URL;

public class LPEntity {
  private final String name;
  private final String lastFmName;
  private final String spotifyLink;
  private final String writeUp;
  private final int ordinal;

  public LPEntity(LPEntity clone) {
    this(clone.getName(), clone.getLastFmName(), clone.getSpotifyLink(), clone.getWriteUp(), clone.getOrdinal());
  }

  public LPEntity(String name, String lastFmName, String spotifyLink, String writeUp, int ordinal) {
    this.name = name;
    this.lastFmName = lastFmName;
    this.spotifyLink = spotifyLink;
    this.writeUp = writeUp;
    this.ordinal = ordinal;
  }

  public String getName() {
    return name;
  }

  public String getLastFmName() {
    return lastFmName;
  }

  public String getSpotifyLink() {
    return spotifyLink;
  }

  public String getWriteUp() {
    return writeUp;
  }

  public int getOrdinal() {
    return ordinal;
  }

  public String getSongId() {
    try {
      URL url = new URL(getSpotifyLink());
      String path = url.getPath();
      return path.substring(path.lastIndexOf('/') + 1);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      return "";
    }
  }
}
