package spotify.lpbot.party.service;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.lastfm.LastFmTrack;
import spotify.lpbot.party.data.lastfm.LastFmUser;
import spotify.util.SpotifyUtils;

@Component
public class LastFmService {
  @Value("${last_fm.api_token}")
  private String lastFmApiToken;

  private WebClient webClient;
  private UriComponentsBuilder lastFmApiUrl;

  @PostConstruct
  void createWebClient() {
    ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    this.webClient = WebClient.builder()
      .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper)))
      .build();

    this.lastFmApiUrl = UriComponentsBuilder.newInstance()
      .scheme("https")
      .host("ws.audioscrobbler.com")
      .path("/2.0")
      .queryParam("api_key", lastFmApiToken)
      .queryParam("format", "json");
  }

  /**
   * Convert the given Spotify Track object into a LastFmTrack object.
   * Note: the resulting object only contains the data required for
   * listening parties, nothing else.
   *
   * @param track the Spotify track
   * @return the last.fm track
   */
  public LastFmTrack getLastFmTrackInfo(Track track) {
    return getLastFmTrackInfo(track, null);
  }

  /**
   * Convert the given Spotify Track object into a LastFmTrack object.
   * If a user is passed as well, will also include the user's scrobble count.
   * Note: the resulting object only contains the data required for
   * listening parties, nothing else.
   *
   * @param track the Spotify track
   * @param lfmUserName (optional) the last.fm username
   * @return the last.fm track
   */
  public LastFmTrack getLastFmTrackInfo(Track track, String lfmUserName) {
    try {
      String artistName = SpotifyUtils.getFirstArtistName(track);
      String trackName = track.getName();
      String url = assembleLastFmApiUrlForTrackGetInfo(artistName, trackName, Optional.ofNullable(lfmUserName));

      return webClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(LastFmTrack.class)
        .block();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Fetch the user from last.fm for the given username.
   * Note: the resulting object only contains the data required for
   * listening parties, nothing else.
   *
   * @param lfmUserName the last.fm username
   * @return the last.fm user
   */
  public LastFmUser getLastFmUserInfo(String lfmUserName) {
    try {
      String url = assembleLastFmApiUrlForUserInfo(lfmUserName);

      return webClient.get()
        .uri(url)
        .retrieve()
        .bodyToMono(LastFmUser.class)
        .block();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private String assembleLastFmApiUrlForTrackGetInfo(String artistName, String trackName, Optional<String> lfmUserName) {
    return lastFmApiUrl.cloneBuilder()
      .queryParam("method", "track.getInfo")
      .queryParam("artist", artistName)
      .queryParam("track", trackName)
      .queryParamIfPresent("username", lfmUserName)
      .build().toUriString();
  }

  private String assembleLastFmApiUrlForUserInfo(String lfmUserName) {
    return lastFmApiUrl.cloneBuilder()
      .queryParam("method", "user.getInfo")
      .queryParam("username", lfmUserName)
      .build().toUriString();
  }
}
