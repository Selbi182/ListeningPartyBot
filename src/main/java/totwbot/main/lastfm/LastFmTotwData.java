package totwbot.main.lastfm;

public class LastFmTotwData {
  public static final LastFmTotwData EMPTY = new LastFmTotwData("", "", 0);

  private final String songLinkUrl;
  private final String profilePictureUrl;
  private final int scrobbleCount;

  public LastFmTotwData(String songLinkUrl, String profilePictureUrl, int scrobbleCount) {
    this.songLinkUrl = songLinkUrl;
    this.profilePictureUrl = profilePictureUrl;
    this.scrobbleCount = scrobbleCount;
  }

  public String getSongLinkUrl() {
    return songLinkUrl;
  }

  public String getProfilePictureUrl() {
    return profilePictureUrl;
  }

  public int getScrobbleCount() {
    return scrobbleCount;
  }
}
