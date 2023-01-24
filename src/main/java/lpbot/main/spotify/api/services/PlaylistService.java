package lpbot.main.spotify.api.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import lpbot.main.spotify.api.SpotifyCall;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

@Service
public class PlaylistService {
  private final static String TRACK_PREFIX = "spotify:track:";

  private final SpotifyApi spotifyApi;

  public PlaylistService(SpotifyApi spotifyApi) {
    this.spotifyApi = spotifyApi;
  }

  /**
   * Add the given list of song IDs to the playlist
   */
  public void addSongsToPlaylistById(Playlist playlist, List<String> trackIds) {
    if (!trackIds.isEmpty()) {
      JsonArray json = new JsonArray();
      for (String id : trackIds) {
        json.add(TRACK_PREFIX + id);
      }
      SpotifyCall.execute(spotifyApi.addItemsToPlaylist(playlist.getId(), json));
    }
  }

  /**
   * Remove every single song from the given playlist
   */
  public void clearPlaylist(Playlist playlist) {
    String playlistId = playlist.getId();
    List<PlaylistTrack> playlistTracks = SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlistId));
    if (!playlistTracks.isEmpty()) {
      JsonArray json = new JsonArray();
      for (int i = 0; i < playlistTracks.size(); i++) {
        JsonObject object = new JsonObject();
        object.addProperty("uri", TRACK_PREFIX + playlistTracks.get(i).getTrack().getId());
        JsonArray positions = new JsonArray();
        positions.add(i);
        object.add("positions", positions);
        json.add(object);
      }

      SpotifyCall.execute(spotifyApi.removeItemsFromPlaylist(playlistId, json));
    }
  }

  /**
   * Get all playlists by the current user
   */
  public List<PlaylistSimplified> getCurrentUsersPlaylists() {
    return SpotifyCall.executePaging(spotifyApi.getListOfCurrentUsersPlaylists());
  }

  /**
   * Create a new playlist with the given name for the current user and return it
   *
   */
  public Playlist createPlaylist(String title) {
    User currentUser = SpotifyCall.execute(spotifyApi.getCurrentUsersProfile());
    return SpotifyCall.execute(spotifyApi.createPlaylist(currentUser.getId(), title));
  }

  /**
   * Convert a PlaylistSimplified to a fully-fledged Playlist
   */
  public Playlist upgradePlaylistSimplified(PlaylistSimplified playlistSimplified) {
    return SpotifyCall.execute(spotifyApi.getPlaylist(playlistSimplified.getId()));
  }

  /**
   * Get all Tracks from the given playlist
   */
  public List<Track> getAllPlaylistTracks(Playlist playlist) {
    return SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlist.getId()))
        .stream()
        .map(p -> (Track) p.getTrack())
        .collect(Collectors.toList());
  }
}
