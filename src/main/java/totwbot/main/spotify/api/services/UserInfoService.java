package totwbot.main.spotify.api.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.enums.ModelObjectType;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import totwbot.main.spotify.api.SpotifyCall;
import totwbot.main.spotify.util.BotLogger;
import totwbot.main.spotify.util.BotUtils;

@Service
public class UserInfoService {

	private final static int MAX_FOLLOWED_ARTIST_FETCH_LIMIT = 50;

	@Autowired
	private SpotifyApi spotifyApi;

	@Autowired
	private BotLogger log;

	/**
	 * Get all the user's followed artists
	 * 
	 * @return
	 */
	public List<String> getFollowedArtistsIds() {
		List<Artist> followedArtists = SpotifyCall.executePaging(spotifyApi
			.getUsersFollowedArtists(ModelObjectType.ARTIST)
			.limit(MAX_FOLLOWED_ARTIST_FETCH_LIMIT));
		List<String> followedArtistIds = followedArtists.stream().map(Artist::getId).collect(Collectors.toList());
		BotUtils.removeNullStrings(followedArtistIds);
		if (followedArtistIds.isEmpty()) {
			log.warning("No followed artists found!");
		}
		return followedArtistIds;
	}
}
