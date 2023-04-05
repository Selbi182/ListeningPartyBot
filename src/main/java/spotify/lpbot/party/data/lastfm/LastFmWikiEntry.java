package spotify.lpbot.party.data.lastfm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LastFmWikiEntry {
  private String published;
  private String summary;

  public LastFmWikiEntry() {
  }

  public String getPublished() {
    return published;
  }

  public String getSummary() {
    return summary;
  }

  public void setPublished(String published) {
    this.published = published;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
