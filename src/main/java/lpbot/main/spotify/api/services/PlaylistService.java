package lpbot.main.spotify.api.services;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import lpbot.main.spotify.api.BotException;
import lpbot.main.spotify.api.SpotifyCall;
import se.michaelthelin.spotify.model_objects.specification.Track;

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
  public void addSongsToPlaylistById(String playlistId, List<String> trackIds) {
    if (!trackIds.isEmpty()) {
      JsonArray json = new JsonArray();
      for (String id : trackIds) {
        json.add(TRACK_PREFIX + id);
      }
      SpotifyCall.execute(spotifyApi.addItemsToPlaylist(playlistId, json));
    }
  }

  /**
   * Remove every single song from the given playlist
   */
  public void clearPlaylist(String playlistId) {
    List<PlaylistTrack> playlistTracks = SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlistId));
    if (playlistTracks.isEmpty()) {
      return;
    }

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

  /**
   * Get the ID from the given parameter
   */
  public String extractId(String input) {
    try {
      // Valid URL was passed
      URI uri = URI.create(input);
      String path = uri.getPath();
      String[] splitPath = path.split("/");
      if (splitPath.length >= 2) {
        String id = splitPath[2];
        switch (splitPath[1]) {
          case "album":
            Album album = SpotifyCall.execute(spotifyApi.getAlbum(id));
            return album.getId();
          case "playlist":
            Playlist playlist = SpotifyCall.execute(spotifyApi.getPlaylist(id));
            return playlist.getId();
        }
      }
    } catch (Exception e) {
      // Unrecognizable
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Check if the playlist with the given ID exists
   */
  public boolean isValidPlaylistId(String playlistId) {
    try {
      SpotifyCall.execute(spotifyApi.getPlaylist(playlistId));
      return true;
    } catch (BotException e) {
      return false;
    }
  }
}
