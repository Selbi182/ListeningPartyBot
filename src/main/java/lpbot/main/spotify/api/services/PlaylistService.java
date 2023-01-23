package lpbot.main.spotify.api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import lpbot.main.spotify.api.BotException;
import lpbot.main.spotify.api.SpotifyCall;

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
