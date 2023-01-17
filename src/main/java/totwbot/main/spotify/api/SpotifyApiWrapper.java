package totwbot.main.spotify.api;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import totwbot.main.spotify.config.Config;

@Configuration
public class SpotifyApiWrapper {

	@Autowired
	private Config config;

	/**
	 * Creates a SpotifyApi instance with the most common settings. A
	 * preconfiguration from the settings is taken first.
	 * 
	 * @return the API instance
	 */
	@Bean
	SpotifyApi spotifyApi() {
		SpotifyApi spotifyApi = new SpotifyApi.Builder()
			.setClientId(config.spotifyBotConfig().getClientId())
			.setClientSecret(config.spotifyBotConfig().getClientSecret())
			.setRedirectUri(generateRedirectUri())
			.build();
		spotifyApi.setAccessToken(config.spotifyBotConfig().getAccessToken());
		spotifyApi.setRefreshToken(config.spotifyBotConfig().getRefreshToken());
		return spotifyApi;
	}
	
	private URI generateRedirectUri() {
		String localhost = "http://localhost:";
		int port = 8185; // TODO find the port automatically
		String loginCallbackUri = SpotifyApiAuthorization.LOGIN_CALLBACK_URI;
		return SpotifyHttpManager.makeUri(localhost + port + loginCallbackUri);
	}
}
