package spotify.lpbot.party.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.api.SpotifyCall;
import spotify.lpbot.party.data.TotwData;
import spotify.util.BotLogger;
import spotify.util.BotUtils;

@Component
public class LastFmService {
  private final SpotifyApi spotifyApi;
  private UriComponentsBuilder lastFmApiUrl;

  LastFmService(SpotifyApi spotifyApi, BotLogger botLogger) {
    this.spotifyApi = spotifyApi;

    try {
      String lastFmApiToken = readToken();
      this.lastFmApiUrl = UriComponentsBuilder.newInstance()
        .scheme("http")
        .host("ws.audioscrobbler.com")
        .path("/2.0")
        .queryParam("api_key", lastFmApiToken)
        .queryParam("format", "json");
    } catch (IOException e) {
      botLogger.error("Failed to start bot! (Couldn't read last.fm token). Terminating...");
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void attachLastFmData(TotwData.Entry totwEntryPartial) throws IOException {
    String lastFmName = totwEntryPartial.getLastFmName();

    // User info
    String lastFmApiUrlForUserInfo = assembleLastFmApiUrlForUserInfo(lastFmName);
    JsonObject jsonUser = executeRequest(lastFmApiUrlForUserInfo, "user");
    if (jsonUser != null) {
      String userPageUrl = jsonUser.get("url").getAsString();
      totwEntryPartial.setUserPageUrl(userPageUrl);
      String userImageUrl = jsonUser.get("image").getAsJsonArray().get(0).getAsJsonObject().get("#text").getAsString();
      totwEntryPartial.setProfilePictureUrl(userImageUrl);
    }

    // Track info
    Track spotifyTrack = SpotifyCall.execute(spotifyApi.getTrack(totwEntryPartial.getSongId()));
    String artistName = BotUtils.getFirstArtistName(spotifyTrack);
    String trackName = spotifyTrack.getName();
    String url = assembleLastFmApiUrlForTrackGetInfo(lastFmName, artistName, trackName);

    JsonObject jsonTrack = executeRequest(url, "track");
    if (jsonTrack != null) {
      String songUrl = jsonTrack.get("url").getAsString();
      totwEntryPartial.setSongLinkUrl(songUrl);
      int scrobbleCount = jsonTrack.get("userplaycount").getAsInt();
      totwEntryPartial.setScrobbleCount(scrobbleCount);

      int globalScrobbleCount = jsonTrack.get("playcount").getAsInt();
      totwEntryPartial.setGlobalScrobbleCount(globalScrobbleCount);
    }
  }

  private JsonObject executeRequest(String url, String rootElement) {
    try {
      String rawJson = Jsoup.connect(url).ignoreContentType(true).execute().body();
      JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
      return json.get(rootElement).getAsJsonObject();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private String assembleLastFmApiUrlForUserInfo(String lfmUserName) {
    return lastFmApiUrl.cloneBuilder()
        .queryParam("method", "user.getInfo")
        .queryParam("username", lfmUserName)
        .build().toUriString();
  }

  private String assembleLastFmApiUrlForTrackGetInfo(String lfmUserName, String artistName, String trackName) {
    return lastFmApiUrl.cloneBuilder()
        .queryParam("method", "track.getInfo")
        .queryParam("username", lfmUserName)
        .queryParam("artist", escape(artistName))
        .queryParam("track", escape(trackName))
        .build().toUriString();
  }

  private String escape(String lfmUserName) {
    return UriUtils.encode(lfmUserName, StandardCharsets.UTF_8);
  }

  private String readToken() throws IOException {
    File tokenFile = new File("./lastfmtoken.txt");
    if (tokenFile.canRead()) {
      return Files.asCharSource(tokenFile, Charset.defaultCharset()).readFirstLine();
    }
    throw new IOException("Can't read token file!");
  }
}
