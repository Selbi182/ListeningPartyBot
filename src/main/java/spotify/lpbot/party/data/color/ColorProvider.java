package spotify.lpbot.party.data.color;

import de.selbi.colorfetch.data.ColorFetchResult;

public interface ColorProvider {
  float NORMALIZE = 0.5f;

  ColorFetchResult getDominantColorFromImageUrl(String artworkUrl);
}
