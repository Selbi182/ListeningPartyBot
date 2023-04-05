package spotify.lpbot.party.data.lastfm;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("user")
public class LastFmUser {
  private String url;

  @JsonProperty("image")
  private List<Image> images;

  public LastFmUser() {}

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<Image> getImages() {
    return images;
  }

  public void setImages(List<Image> images) {
    this.images = images;
  }

  @JsonIgnore
  public String getSmallImageUrl() {
    return images.stream()
      .filter(img -> "small".equals(img.getSize()))
      .findFirst()
      .map(Image::getUrl)
      .orElse(null);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Image {
    private String size;

    @JsonProperty("#text")
    private String url;

    public String getSize() {
      return size;
    }

    public String getUrl() {
      return url;
    }

    public void setSize(String size) {
      this.size = size;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }
}
