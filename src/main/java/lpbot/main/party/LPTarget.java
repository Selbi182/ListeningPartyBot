package lpbot.main.party;

import java.util.List;

import se.michaelthelin.spotify.model_objects.AbstractModelObject;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class LPTarget {
  private enum Type {
    ALBUM,
    PLAYLIST
  }

  private final Type type;

  private Playlist playlist;
  private Album album;

  private final List<Track> tracks;

  public LPTarget(AbstractModelObject abstractModelObject, List<Track> tracks) {
    if (abstractModelObject instanceof Album) {
      this.type = Type.ALBUM;
      this.album = (Album) abstractModelObject;
    } else if (abstractModelObject instanceof Playlist) {
      this.type = Type.PLAYLIST;
      this.playlist = (Playlist) abstractModelObject;
    } else {
      throw new IllegalArgumentException("Invalid AbstractModelObject");
    }
    this.tracks = tracks;
  }

  public String getId() {
    if (type.equals(Type.ALBUM)) {
      return album.getId();
    }
    return playlist.getId();
  }

  public String getUri() {
    if (type.equals(Type.ALBUM)) {
      return album.getExternalUrls().get("spotify");
    }
    return playlist.getExternalUrls().get("spotify");
  }

  public List<Track> getTracks() {
    return tracks;
  }
}
