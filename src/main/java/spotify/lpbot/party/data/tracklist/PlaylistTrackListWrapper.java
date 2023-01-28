package spotify.lpbot.party.data.tracklist;

import java.awt.Color;
import java.util.List;

import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class PlaylistTrackListWrapper implements TrackListWrapper {
  private final Playlist playlist;
  private final List<Track> tracks;
  private final List<Color> colors;

  public PlaylistTrackListWrapper(Playlist playlist, List<Track> tracks, List<Color> colors) {
    this.playlist = playlist;
    this.tracks = tracks;
    this.colors = colors;
  }

  @Override
  public String getLink() {
    return playlist.getExternalUrls().get("spotify");
  }

  @Override
  public List<Track> getTracks() {
    return tracks;
  }

  @Override
  public Color getColorByTrackIndex(int index) {
    return colors.get(index);
  }
}
