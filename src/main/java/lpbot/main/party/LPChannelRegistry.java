package lpbot.main.party;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import lpbot.main.spotify.api.SpotifyCall;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Album;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.TrackSimplified;

@Component
public class LPChannelRegistry {
  private final SpotifyApi spotifyApi;
  private final LPHandler lpHandler;

  private final Map<Long, LPInstance> lpInstancesForChannelId;

  public LPChannelRegistry(SpotifyApi spotifyApi, LPHandler lpHandler) {
    this.spotifyApi = spotifyApi;
    this.lpHandler = lpHandler;
    this.lpInstancesForChannelId = new ConcurrentHashMap<>();
  }

  public Optional<LPInstance> getExistingLPInstance(TextChannel textChannel) {
    if (isRegistered(textChannel)) {
      return Optional.of(lpInstancesForChannelId.get(textChannel.getId()));
    } else {
      return Optional.empty();
    }
  }

  public boolean isRegistered(TextChannel textChannel) {
    return lpInstancesForChannelId.containsKey(textChannel.getId());
  }

  public LPInstance register(TextChannel textChannel, String potentialPlaylist) {
    if (!isRegistered(textChannel) || !lpInstancesForChannelId.get(textChannel.getId()).isStarted()) {
      LPTarget target = verifyUriAndCreateTarget(potentialPlaylist);
      if (target != null) {
        LPInstance lpInstance = new LPInstance(textChannel, target, lpHandler);
        lpInstancesForChannelId.put(textChannel.getId(), lpInstance);
        return lpInstance;
      }
    }
    return null;
  }

  public LPTarget verifyUriAndCreateTarget(String input) {
    try {
      // Valid URL was passed
      URI uri = URI.create(input);
      String path = uri.getPath();
      String[] splitPath = path.split("/");
      if (splitPath.length >= 2) {
        String id = splitPath[2];
        switch (splitPath[1]) {
        case "album":
          Album album = SpotifyCall.execute(spotifyApi.getAlbum(id));
          List<TrackSimplified> albumTracks = SpotifyCall.executePaging(spotifyApi.getAlbumsTracks(album.getId()));
          List<Track> allAlbumTracks = new ArrayList<>();
          for (List<TrackSimplified> sublistTracks : Lists.partition(albumTracks, 50)) {
            String[] ids = sublistTracks.stream().map(TrackSimplified::getId).toArray(String[]::new);
            Track[] execute = SpotifyCall.execute(spotifyApi.getSeveralTracks(ids));
            allAlbumTracks.addAll(Arrays.asList(execute));
          }
          return new LPTarget(album, allAlbumTracks);
        case "playlist":
          Playlist playlist = SpotifyCall.execute(spotifyApi.getPlaylist(id));
          List<Track> allPlaylistTracks = SpotifyCall.executePaging(spotifyApi.getPlaylistsItems(playlist.getId()))
              .stream()
              .map(p -> (Track) p.getTrack())
              .collect(Collectors.toList());
          return new LPTarget(playlist, allPlaylistTracks);
        }
      }
    } catch (Exception e) {
      // Unrecognizable
      e.printStackTrace();
    }
    return null;
  }
}
