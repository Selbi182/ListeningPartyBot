package totwbot.main.spotify.api;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import totwbot.main.spotify.api.events.LoggedInEvent;
import totwbot.main.spotify.config.Config;
import totwbot.main.spotify.util.BotLogger;

@Component
@RestController
public class SpotifyApiAuthorization {

	protected final static String LOGIN_CALLBACK_URI = "/login-callback";
	private final static String SCOPES = "user-library-read playlist-modify-public playlist-modify-private";
	private final static long LOGIN_TIMEOUT = 10;

	private final SpotifyApi spotifyApi;
	private final Config config;
	private final BotLogger log;
	private final ApplicationEventPublisher applicationEventPublisher;

	public SpotifyApiAuthorization(SpotifyApi spotifyApi, Config config, BotLogger botLogger, ApplicationEventPublisher applicationEventPublisher) {
		this.spotifyApi = spotifyApi;
		this.config = config;
		this.log = botLogger;
		this.applicationEventPublisher = applicationEventPublisher;
	}
	
	@PostConstruct
	private void initSpotifyCall() {
		SpotifyCall.spotifyApiAuthorization = this;
	}

	/////////////////////

	@EventListener(ApplicationReadyEvent.class)
	public void initialLogin() {
		refresh();
		applicationEventPublisher.publishEvent(new LoggedInEvent(this));
	}


	public String refresh() {
		try {
			return authorizationCodeRefresh();
		} catch (HttpConnectTimeoutException e) {
			authenticate();
			return refresh();
		}
	}

	///////////////////////

	/**
	 * Authentication mutex to be used while the user is being prompted to log in
	 */
	private static final Semaphore lock = new Semaphore(0);

	/**
	 * Authentication process
	 */
	private void authenticate() {
		URI uri = SpotifyCall.execute(spotifyApi.authorizationCodeUri().scope(SCOPES));
		try {
			if (!Desktop.isDesktopSupported()) {
				throw new HeadlessException();
			}
			Desktop.getDesktop().browse(uri);
		} catch (IOException | HeadlessException e) {
			log.warning("Couldn't open browser window. Please login at this URL:");
			System.out.println(uri.toString());
		}
		try {
			boolean lockAcquired = lock.tryAcquire(LOGIN_TIMEOUT, TimeUnit.MINUTES);
			if (!lockAcquired) {
				throw new IllegalStateException("Couldn't acquired lock");
			}
		} catch (IllegalStateException | InterruptedException e) {
			log.error("Login timeout! Shutting down application in case of a Spotify Web API anomaly!");
			System.exit(1);
		}
	}

	/**
	 * Callback receiver for logins
	 */
	@RequestMapping(LOGIN_CALLBACK_URI)
	private ResponseEntity<String> loginCallback(@RequestParam String code) {
		AuthorizationCodeCredentials acc = SpotifyCall.execute(spotifyApi.authorizationCode(code));
		updateTokens(acc);
		lock.release();
		return new ResponseEntity<>("Successfully logged in!", HttpStatus.OK);
	}

	///////////////////////

	/**
	 * Refresh the access token
	 */
	private String authorizationCodeRefresh() throws HttpConnectTimeoutException {
		try {
			AuthorizationCodeCredentials acc = Executors.newSingleThreadExecutor()
					.submit(() -> SpotifyCall.execute(spotifyApi.authorizationCodeRefresh()))
					.get(LOGIN_TIMEOUT, TimeUnit.SECONDS);
			updateTokens(acc);
			return acc.getAccessToken();
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			String msg = "Failed to automatically refresh access token after " + LOGIN_TIMEOUT
					+ " seconds. A manual (re-)login might be required.";
			log.error(msg);
			throw new HttpConnectTimeoutException(msg);
		}
	}

	/**
	 * Store the access and refresh tokens in the settings
	 */
	private void updateTokens(AuthorizationCodeCredentials acc) {
		String accessToken = spotifyApi.getAccessToken();
		if (acc.getAccessToken() != null) {
			accessToken = acc.getAccessToken();
		}
		String refreshToken = spotifyApi.getRefreshToken();
		if (acc.getRefreshToken() != null) {
			refreshToken = acc.getRefreshToken();
		}

		spotifyApi.setAccessToken(accessToken);
		spotifyApi.setRefreshToken(refreshToken);
		try {
			config.updateTokens(accessToken, refreshToken);
		} catch (IOException e) {
			log.error("Failed to update tokens in the properties file! These will get lost during a server restart.");
			e.printStackTrace();
		}
	}
}
