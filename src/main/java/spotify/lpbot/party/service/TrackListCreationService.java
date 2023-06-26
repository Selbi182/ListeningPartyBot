package spotify.lpbot.party.service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import spotify.api.SpotifyApiException;
import spotify.api.SpotifyCall;
import spotify.lpbot.party.data.color.ColorService;
import spotify.lpbot.party.data.tracklist.AlbumTrackListWrapper;
import spotify.lpbot.party.data.tracklist.PlaylistTrackListWrapper;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.util.SpotifyUtils;

@Component
public class TrackListCreationService {
  private static final int MAXIMUM_TRACK_COUNT = 100;

  private final SpotifyApi spotifyApi;
  private final ColorService colorService;

  TrackListCreationService(SpotifyApi spotifyApi, ColorService colorService) {
    this.spotifyApi = spotifyApi;
    this.colorService = colorService;
  }

  public TrackListWrapper verifyUriAndCreateTarget(String potentialUrl) {
    try {
      if (SpotifyUtils.isShortSpotifyUrl(potentialUrl)) {
        potentialUrl = SpotifyUtils.getFullUrlFromShortSpotifyUrl(potentialUrl);
      }
      URI uri = URI.create(potentialUrl);
      if (!uri.isAbsolute()) {
        throw new IllegalArgumentException("Invalid URL");
      }
      String path = uri.getPath();
      String[] splitPath = path.split("/");
      if (splitPath.length >= 2) {
        String id = splitPath[splitPath.length - 1];
        String releaseType = splitPath[splitPath.length - 2].toLowerCase();
        switch (releaseType) {
          case "album":
            Album album = SpotifyCall.execute(spotifyApi.getAlbum(id));
            return createAlbumTracklist(album);
          case "playlist":
            Playlist playlist = SpotifyCall.execute(spotifyApi.getPlaylist(id));
            return createPlaylistTracklist(playlist);
          case "track":
            Track track = SpotifyCall.execute(spotifyApi.getTrack(id));
            Album potentialOneTrackAlbum = SpotifyCall.execute(spotifyApi.getAlbum(track.getAlbum().getId()));
            if (potentialOneTrackAlbum.getTracks().getTotal() == 1) {
              return createAlbumTracklist(potentialOneTrackAlbum);
            }
            // fallthrough if not a one-track album
          default:
            throw new IllegalArgumentException(String.format("Release type `%s` is not supported (must be `album` or `playlist`)", releaseType));
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse shortened URL", e);
    }
    throw new IllegalArgumentException("Failed to parse Spotify URL");
  }

  private TrackListWrapper createAlbumTracklist(Album album) {
    try {
      verifyBelowTrackCountLimit(album.getTracks().getTotal());
      List<TrackSimplified> albumTracks = SpotifyCall.executePaging(spotifyApi.getAlbumsTracks(album.getId()));
      List<Track> allAlbumTracks = new ArrayList<>();
      for (List<TrackSimplified> sublistTracks : SpotifyUtils.partitionList(albumTracks, 50)) {
        String[] ids = sublistTracks.stream().map(TrackSimplified::getId).toArray(String[]::new);
        Track[] execute = SpotifyCall.execute(spotifyApi.getSeveralTracks(ids));
        allAlbumTracks.addAll(Arrays.asList(execute));
      }
      String smallestImage = SpotifyUtils.findSmallestImage(album.getImages());
      ColorFetchResult albumColor = colorService.getDominantColorFromImageUrl(smallestImage);
      return new AlbumTrackListWrapper(album, allAlbumTracks, albumColor.getPrimary());
    } catch (SpotifyApiException e) {
      throw new IllegalArgumentException("Invalid URL (no Spotify release found or malformed formatting)");
    }
  }

  private TrackListWrapper createPlaylistTracklist(Playlist playlist) {
    try {
      verifyBelowTrackCountLimit(playlist.getTracks().getTotal());
      List<Track> allPlaylistTracks = SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlist.getId()))
        .stream()
        .map(p -> (Track) p.getTrack())
        .collect(Collectors.toList());
      List<ColorFetchResult.RGB> colors = allPlaylistTracks.stream()
        .map(Track::getAlbum)
        .map(AlbumSimplified::getImages)
        .map(SpotifyUtils::findSmallestImage)
        .map(colorService::getDominantColorFromImageUrl)
        .map(ColorFetchResult::getPrimary)
        .collect(Collectors.toList());
      return new PlaylistTrackListWrapper(playlist, allPlaylistTracks, colors);
    } catch (SpotifyApiException e) {
      throw new IllegalArgumentException("Invalid URL (no Spotify release found or malformed formatting)");
    }
  }

  private void verifyBelowTrackCountLimit(int trackCount) {
    if (trackCount > MAXIMUM_TRACK_COUNT) {
      throw new IllegalStateException("Track count (" + trackCount + ") exceeds the maximum (" + MAXIMUM_TRACK_COUNT + ")");
    }
  }
}
