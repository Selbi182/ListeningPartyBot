package spotify.lpbot.party.totw;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class TotwEntity {
  private final String headline;
  private final int participants;
  private final List<Partial> totwEntities;

  public TotwEntity(String headline, int participants, List<Partial> totwEntities) {
    this.headline = headline;
    this.participants = participants;
    this.totwEntities = totwEntities;
  }

  public String getHeadline() {
    return headline;
  }

  public int getParticipants() {
    return participants;
  }

  public List<Partial> getTotwEntities() {
    return totwEntities;
  }

  public static class Partial {
    private final String name;
    private final String lastFmName;
    private final String spotifyLink;
    private final String writeUp;
    private final int ordinal;

    public Partial(Partial clone) {
      this(clone.getName(), clone.getLastFmName(), clone.getSpotifyLink(), clone.getWriteUp(), clone.getOrdinal());
    }

    public Partial(String name, String lastFmName, String spotifyLink, String writeUp, int ordinal) {
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

  public static class Full extends Partial {
    private String songLinkUrl;
    private String userPageUrl;
    private String profilePictureUrl;
    private Integer scrobbleCount;

    public Full(Partial lpEntity) {
      super(lpEntity);
    }

    public void setSongLinkUrl(String songLinkUrl) {
      this.songLinkUrl = songLinkUrl;
    }

    public void setUserPageUrl(String userPageUrl) {
      this.userPageUrl = userPageUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
      this.profilePictureUrl = profilePictureUrl;
    }

    public void setScrobbleCount(Integer scrobbleCount) {
      this.scrobbleCount = scrobbleCount;
    }

    public String getSongLinkUrl() {
      return songLinkUrl;
    }

    public String getUserPageUrl() {
      return userPageUrl;
    }

    public String getProfilePictureUrl() {
      return profilePictureUrl;
    }

    public Integer getScrobbleCount() {
      return scrobbleCount;
    }
  }
}
