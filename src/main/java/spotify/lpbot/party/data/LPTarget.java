package spotify.lpbot.party.data;

import java.util.List;

import spotify.lpbot.party.totw.TotwEntity;
import se.michaelthelin.spotify.model_objects.AbstractModelObject;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;

public class LPTarget {
  private enum Type {
    ALBUM,
    PLAYLIST
  }

  private final List<Track> tracks;
  private final Type type;

  private Playlist playlist;
  private Album album;

  private TotwEntity totwData;

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

  public LPTarget(AbstractModelObject abstractModelObject, List<Track> tracks, TotwEntity totwData) {
    this(abstractModelObject, tracks);
    this.totwData = totwData;
  }

  public String getLink() {
    if (type.equals(Type.ALBUM)) {
      return album.getExternalUrls().get("spotify");
    }
    return playlist.getExternalUrls().get("spotify");
  }

  public List<Track> getTracks() {
    return tracks;
  }

  public TotwEntity getTotwData() {
    return totwData;
  }
}
