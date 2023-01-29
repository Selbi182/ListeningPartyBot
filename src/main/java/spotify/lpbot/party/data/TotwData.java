package spotify.lpbot.party.data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class TotwData {
  private final String headline;
  private final List<TotwData.Entry> totwEntries;

  public TotwData(String headline, List<TotwData.Entry> totwEntries) {
    this.headline = headline;
    this.totwEntries = totwEntries;
  }

  public String getHeadline() {
    return headline;
  }

  public List<String> getParticipants() {
    return totwEntries.stream()
      .map(Entry::getName)
      .distinct()
      .collect(Collectors.toList());
  }

  public List<TotwData.Entry> getTotwEntries() {
    return totwEntries;
  }

  public static class Entry {
    // Entry data (taken from the submission form)
    private final String name;
    private final String lastFmName;
    private final String spotifyLink;
    private final String writeUp;

    // Full data (taken from the last.fm API)
    private String songLinkUrl;
    private String userPageUrl;
    private String profilePictureUrl;
    private Integer scrobbleCount;
    private Integer globalScrobbleCount;

    public Entry(String name, String lastFmName, String spotifyLink, String writeUp) {
      this.name = name;
      this.lastFmName = lastFmName;
      this.spotifyLink = spotifyLink;
      this.writeUp = writeUp;
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

    ////////////////////////////

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

    public void setGlobalScrobbleCount(Integer globalScrobbleCount) {
      this.globalScrobbleCount = globalScrobbleCount;
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

    public Integer getGlobalScrobbleCount() {
      return globalScrobbleCount;
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

}
