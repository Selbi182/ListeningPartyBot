package spotify.lpbot.party.data.tracklist;

import java.awt.Color;
import java.util.List;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class AlbumTrackListWrapper implements TrackListWrapper {
  private final Album album;
  private final List<Track> tracks;
  private final ColorFetchResult.RGB color;

  public AlbumTrackListWrapper(Album album, List<Track> tracks, ColorFetchResult.RGB color) {
    this.album = album;
    this.tracks = tracks;
    this.color = color;
  }

  @Override
  public String getLink() {
    return album.getExternalUrls().get("spotify");
  }

  @Override
  public List<Track> getTracks() {
    return tracks;
  }

  @Override
  public ColorFetchResult.RGB getColorByTrackIndex(int index) {
    return color; // album only has a single image, therefore only one color; index is irrelevant
  }
}
