package spotify.lpbot.party.data.color;

import de.selbi.colorfetch.data.ColorFetchResult;

public interface ColorProvider {
  String NORMALIZE = "0.5";

  ColorFetchResult getDominantColorFromImageUrl(String artworkUrl);
}
