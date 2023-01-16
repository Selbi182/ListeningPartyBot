package totwbot.spotify.api.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.Playlist;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;

import totwbot.spotify.api.BotException;
import totwbot.spotify.api.SpotifyCall;
import totwbot.spotify.util.BotUtils;

@Service
public class PlaylistService {
	private final static int PLAYLIST_ADD_LIMIT = 100;
	private final static int PLAYLIST_SIZE_LIMIT = 10000;
	private final static String TRACK_PREFIX = "spotify:track:";

	@Autowired
	private SpotifyApi spotifyApi;

	/**
	 * Add the given list of song IDs to the playlist
	 *
	 * @return
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
	 * @param playlistId
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
	 * @param playlistId
	 * @return
	 */
	public boolean isValidPlaylistId(String playlistId) {
		try {
			SpotifyCall.execute(spotifyApi.getPlaylist(playlistId));
			return true;
		} catch (BotException e) {
			return false;
		}
	}

	/**
	 * Check if circular playlist fitting is required (if enabled; otherwise an
	 * exception is thrown)
	 * 
	 * @param playlistId
	 * @param songsToAddCount
	 * @return true on success, false if playlist is full and can't be cleared
	 */
	private boolean circularPlaylistFitting(String playlistId, int songsToAddCount) {
		Playlist p = SpotifyCall.execute(spotifyApi.getPlaylist(playlistId));
		final int currentPlaylistCount = p.getTracks().getTotal();
		if (currentPlaylistCount + songsToAddCount > PLAYLIST_SIZE_LIMIT) {
			deleteSongsFromBottomOnLimit(playlistId, currentPlaylistCount, songsToAddCount);
		}
		return true;
	}

	/**
	 * Delete as many songs from the bottom as necessary to make room for any new
	 * songs to add, as Spotify playlists have a fixed limit of 10000 songs.
	 * 
	 * If circularPlaylistFitting isn't enabled, an exception is thrown on a full
	 * playlist instead.
	 * 
	 * @param playlistId
	 * @param currentPlaylistCount
	 * @param songsToAddCount
	 */
	private void deleteSongsFromBottomOnLimit(String playlistId, int currentPlaylistCount, int songsToAddCount) {
		int totalSongsToDeleteCount = currentPlaylistCount + songsToAddCount - PLAYLIST_SIZE_LIMIT;
		boolean repeat = totalSongsToDeleteCount > PLAYLIST_ADD_LIMIT;
		int songsToDeleteCount = repeat ? PLAYLIST_ADD_LIMIT : totalSongsToDeleteCount;
		final int offset = currentPlaylistCount - songsToDeleteCount;

		List<PlaylistTrack> tracksToDelete = SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlistId).offset(offset).limit(PLAYLIST_ADD_LIMIT));

		JsonArray json = new JsonArray();
		for (int i = 0; i < tracksToDelete.size(); i++) {
			JsonObject object = new JsonObject();
			object.addProperty("uri", TRACK_PREFIX + ((Playlist) tracksToDelete.get(i).getTrack()).getId());
			JsonArray positions = new JsonArray();
			positions.add(currentPlaylistCount - songsToDeleteCount + i);
			object.add("positions", positions);
			json.add(object);
		}

		SpotifyCall.execute(spotifyApi.removeItemsFromPlaylist(playlistId, json));

		// Repeat if more than 100 songs have to be added/deleted (should rarely happen,
		// so a recursion will be slow, but it'll do the job)
		if (repeat) {
			deleteSongsFromBottomOnLimit(playlistId, currentPlaylistCount - 100, songsToAddCount);
		}
	}
}
