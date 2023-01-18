package totwbot.main.spotify.api;

/**
 * A general wrapper for every type of Exception related to outside requests to
 * the Spotify Web API, most commonly (but not limited to) the
 * {@link se.michaelthelin.spotify.exceptions.SpotifyWebApiException}.
 *
 */
public class BotException extends RuntimeException {
	private static final long serialVersionUID = 2306804985486380794L;

	private final Exception baseException;

	public BotException(Exception e) {
		e.printStackTrace();
		this.baseException = e;
	}

	public Exception getBaseException() {
		return baseException;
	}

	@Override
	public String toString() {
		return baseException.toString();
	}
}
