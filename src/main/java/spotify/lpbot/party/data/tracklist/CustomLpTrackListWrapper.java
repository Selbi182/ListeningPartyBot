package spotify.lpbot.party.data.tracklist;

import java.util.List;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.CustomLpData;

public class CustomLpTrackListWrapper extends PlaylistTrackListWrapper {
  private final CustomLpData customLpData;
  public CustomLpTrackListWrapper(Playlist playlist, List<Track> tracks, List<ColorFetchResult.RGB> colors, CustomLpData customLpData) {
    super(playlist, tracks, colors);
    this.customLpData = customLpData;
  }

  public CustomLpData getCustomLpData() {
    return customLpData;
  }
}
