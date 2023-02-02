package spotify.lpbot.party.service;

import java.awt.Color;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import spotify.api.BotException;
import spotify.api.SpotifyCall;
import spotify.lpbot.party.data.tracklist.AlbumTrackListWrapper;
import spotify.lpbot.party.data.tracklist.PlaylistTrackListWrapper;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.util.BotUtils;

@Component
public class TrackListCreationService {
  private static final int MAXIMUM_TRACK_COUNT = 100;

  private final SpotifyApi spotifyApi;
  private final ColorService colorService;

  TrackListCreationService(SpotifyApi spotifyApi, ColorService colorService) {
    this.spotifyApi = spotifyApi;
    this.colorService = colorService;
  }

  public TrackListWrapper verifyUriAndCreateTarget(String input) {
    URI uri = URI.create(input);
    String path = uri.getPath();
    String[] splitPath = path.split("/");
    if (splitPath.length >= 2) {
      String id = splitPath[2];
      switch (splitPath[1]) {
        case "album":
          return createAlbumTracklist(id);
        case "playlist":
          return createPlaylistTracklist(id);
        }
    }
    throw new IllegalArgumentException("Release type is not supported (only albums and playlists are)");
  }

  private TrackListWrapper createAlbumTracklist(String albumId) {
    try {
      Album album = SpotifyCall.execute(spotifyApi.getAlbum(albumId));
      verifyBelowTrackCountLimit(album.getTracks().getTotal());
      List<TrackSimplified> albumTracks = SpotifyCall.executePaging(spotifyApi.getAlbumsTracks(album.getId()));
      List<Track> allAlbumTracks = new ArrayList<>();
      for (List<TrackSimplified> sublistTracks : Lists.partition(albumTracks, 50)) {
        String[] ids = sublistTracks.stream().map(TrackSimplified::getId).toArray(String[]::new);
        Track[] execute = SpotifyCall.execute(spotifyApi.getSeveralTracks(ids));
        allAlbumTracks.addAll(Arrays.asList(execute));
      }
      String smallestImage = BotUtils.findSmallestImage(album.getImages());
      Color albumColor = colorService.getDominantColorFromImage(smallestImage);
      return new AlbumTrackListWrapper(album, allAlbumTracks, albumColor);
    } catch (BotException e) {
      throw new IllegalArgumentException("The provided URL is invalid (no Spotify album detected or malformed formatting)");
    }
  }

  private TrackListWrapper createPlaylistTracklist(String id) {
    try {
      Playlist playlist = SpotifyCall.execute(spotifyApi.getPlaylist(id));
      verifyBelowTrackCountLimit(playlist.getTracks().getTotal());
      List<Track> allPlaylistTracks = SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlist.getId()))
          .stream()
          .map(p -> (Track) p.getTrack())
          .collect(Collectors.toList());
      List<Color> colors = allPlaylistTracks.stream()
          .map(Track::getAlbum)
          .map(AlbumSimplified::getImages)
          .map(BotUtils::findSmallestImage)
          .map(colorService::getDominantColorFromImage)
          .collect(Collectors.toList());
      return new PlaylistTrackListWrapper(playlist, allPlaylistTracks, colors);
    } catch (BotException e) {
      throw new IllegalArgumentException("The provided URL is invalid (no Spotify playlist detected or malformed formatting)");
    }

  }

  private void verifyBelowTrackCountLimit(int trackCount) {
    if (trackCount > MAXIMUM_TRACK_COUNT) {
      throw new IllegalStateException("Track count (" + trackCount + ") exceeds the maximum (" + MAXIMUM_TRACK_COUNT + ")");
    }
  }
}
