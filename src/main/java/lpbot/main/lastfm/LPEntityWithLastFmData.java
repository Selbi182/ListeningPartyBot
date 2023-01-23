package lpbot.main.lastfm;

import lpbot.main.playlist.LPEntity;

public class LPEntityWithLastFmData extends LPEntity {
  private String songLinkUrl;
  private String userPageUrl;
  private String profilePictureUrl;
  private Integer scrobbleCount;

  public LPEntityWithLastFmData(LPEntity lpEntity) {
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
