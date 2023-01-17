package totwbot.main.lastfm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.io.Files;
import com.wrapper.spotify.model_objects.specification.Track;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Chart;
import de.umass.lastfm.ImageSize;
import de.umass.lastfm.LastFmTrack;
import de.umass.lastfm.User;
import totwbot.main.spotify.util.BotLogger;

@Component
public class LastFmDataHandler {
  // Get user data (including name and image): http://ws.audioscrobbler.com/2.0/?method=user.getinfo&user=SellBee&api_key=d4762eb4bcc6a732a477255cbee03f0c&format=json

  // Get weekly charts (with 1 week time range): http://ws.audioscrobbler.com/2.0/?method=user.getweeklytrackchart&user=SellBee&api_key=d4762eb4bcc6a732a477255cbee03f0c&format=json&from=1673359763&to=1673964563
  // from and to must be unix timestamps
  // Results are neatly sorted by

  @Autowired
  private BotLogger log;

  private String lastFmApiToken;

  public LastFmDataHandler() {
    Caller.getInstance().setDebugMode(true);
    try {
      this.lastFmApiToken = readToken();
    } catch (IOException e) {
      log.error("Failed to start bot! (Couldn't read last.fm token). Terminating...");
      e.printStackTrace();
      System.exit(1);
    }
  }

  public LastFmTotwData getLastFmDataForTotw(String lfmUserName, Track spotifyTrack) {
    try {
      User info = User.getInfo(lfmUserName, lastFmApiToken);
      String artistName = spotifyTrack.getArtists()[0].getName();
      LastFmTrack trackInfo = LastFmTrack.getInfo(artistName, spotifyTrack.getName(), null, lfmUserName, lastFmApiToken);
      String profilePictureUrl = info.getImageURL();
      String songLinkUrl = trackInfo.getImageURL(ImageSize.LARGE);
      int scrobbleCount = trackInfo.getPlaycount();
      return new LastFmTotwData(songLinkUrl, profilePictureUrl, scrobbleCount);
    } catch (Exception e) {
      e.printStackTrace();
      return LastFmTotwData.EMPTY;
    }
  }

  private String readToken() throws IOException {
    File tokenFile = new File("./discordtoken.txt");
    if (tokenFile.canRead()) {
      return Files.asCharSource(tokenFile, Charset.defaultCharset()).readFirstLine();
    }
    throw new IOException("Can't read token file!");
  }
}
