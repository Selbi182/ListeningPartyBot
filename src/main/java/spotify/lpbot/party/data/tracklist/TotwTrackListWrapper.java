package spotify.lpbot.party.data.tracklist;

import java.util.List;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.TotwData;

public class TotwTrackListWrapper extends PlaylistTrackListWrapper {
  private final TotwData totwData;
  public TotwTrackListWrapper(Playlist playlist, List<Track> tracks, List<ColorFetchResult.RGB> colors, TotwData totwData) {
    super(playlist, tracks, colors);
    this.totwData = totwData;
  }

  public TotwData getTotwData() {
    return totwData;
  }
}
