package totwbot.main.spotify.api.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.AlbumGroup;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import totwbot.main.spotify.api.BotException;
import totwbot.main.spotify.api.SpotifyCall;
import totwbot.main.spotify.config.Config;

@Service
public class AlbumService {

	private final static int MAX_ALBUM_FETCH_LIMIT = 50;

	@Autowired
	private Config config;

	@Autowired
	private SpotifyApi spotifyApi;

	/**
	 * Fetch all albums of the given artists
	 * 
	 * @param followedArtists
	 * @param albumGroups
	 * @return
	 * @throws IOException 
	 */
	public List<AlbumSimplified> getAllAlbumsOfArtists(List<String> followedArtists) throws IOException {
		Collection<AlbumGroup> enabledAlbumGroups = Arrays.asList(AlbumGroup.values());
		return getAlbumsOfArtists(followedArtists, enabledAlbumGroups);
	}

	/**
	 * Get all album IDs of the given list of artists, mapped into album group
	 * 
	 * @param artists
	 * @return
	 * @throws IOException 
	 */
	private List<AlbumSimplified> getAlbumsOfArtists(List<String> artists, Collection<AlbumGroup> enabledAlbumGroups) throws IOException {
		String albumGroupString = createAlbumGroupString(enabledAlbumGroups);
		List<AlbumSimplified> albums = new ArrayList<>();
		for (String a : artists) {
			List<AlbumSimplified> albumsOfCurrentArtist = getAlbumIdsOfSingleArtist(a, albumGroupString);
			albums.addAll(albumsOfCurrentArtist);
		}
		return albums;
	}

	/**
	 * Creates the comma-delimited, lowercase String of album groups to search for
	 * 
	 * @param enabledAlbumGroups
	 * @return
	 */
	private String createAlbumGroupString(Collection<AlbumGroup> enabledAlbumGroups) {
		StringJoiner albumGroupsAsString = new StringJoiner(",");
		for (AlbumGroup ag : enabledAlbumGroups) {
			albumGroupsAsString.add(ag.getGroup());
		}
		return albumGroupsAsString.toString();
	}

	/**
	 * Return the albums of a single given artist
	 * 
	 * @param artistId
	 * @param albumGroup
	 * @return
	 */
	private List<AlbumSimplified> getAlbumIdsOfSingleArtist(String artistId, String albumGroups) {
		return SpotifyCall.executePaging(spotifyApi
			.getArtistsAlbums(artistId)
			.market(config.spotifyBotConfig().getMarket())
			.limit(MAX_ALBUM_FETCH_LIMIT)
			.album_type(albumGroups));
	}
}
