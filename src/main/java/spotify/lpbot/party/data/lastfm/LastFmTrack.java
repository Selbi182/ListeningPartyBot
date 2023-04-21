package spotify.lpbot.party.data.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("track")
public class LastFmTrack {
  private String name;
  private String url;
  private long duration;

  @JsonProperty("playcount")
  private Integer scrobbleCount;

  @JsonProperty("userplaycount")
  private Integer userScrobbleCount;

  @JsonProperty("wiki")
  private LastFmWikiEntry lastFmWikiEntry;

  public LastFmTrack() {
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public long getDuration() {
    return duration;
  }

  public Integer getScrobbleCount() {
    return scrobbleCount;
  }

  public Integer getUserScrobbleCount() {
    return userScrobbleCount;
  }

  public LastFmWikiEntry getWiki() {
    return lastFmWikiEntry;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setScrobbleCount(Integer scrobbleCount) {
    this.scrobbleCount = scrobbleCount;
  }

  public void setUserScrobbleCount(Integer userScrobbleCount) {
    this.userScrobbleCount = userScrobbleCount;
  }

  public void setWiki(LastFmWikiEntry lastFmWikiEntry) {
    this.lastFmWikiEntry = lastFmWikiEntry;
  }

  @JsonIgnore
  public boolean hasWiki() {
    return lastFmWikiEntry != null;
  }

  @JsonIgnore
  public boolean hasScrobbles() {
    return scrobbleCount > 0;
  }

  @JsonIgnore
  public boolean hasUserScrobbles() {
    return userScrobbleCount != null && userScrobbleCount > 0;
  }

  @JsonIgnore
  public String getWikiUrl() {
    return getUrl() + "/+wiki";
  }
}
