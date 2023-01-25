package spotify.lpbot.party.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import spotify.lpbot.party.data.LPTarget;
import spotify.lpbot.party.totw.TotwEntity;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.services.PlaylistService;

@Service
public class TotwPlaylistService {
  private final PlaylistService playlistService;

  public TotwPlaylistService(PlaylistService playlistService) {
    this.playlistService = playlistService;
  }

  public LPTarget findOrCreateTotwPlaylist(TotwEntity totwData) {
    // Check if a playlist already exists
    List<PlaylistSimplified> userPlaylists = playlistService.getCurrentUsersPlaylists();
    Optional<PlaylistSimplified> previousTotwPlaylist = userPlaylists.stream()
        .filter(p -> p.getName().equals(totwData.getHeadline()))
        .findFirst();

    // If it doesn't create it. If it does, clear it
    Playlist playlist;
    if (previousTotwPlaylist.isEmpty()) {
      playlist = playlistService.createPlaylist(totwData.getHeadline());
    } else {
      playlist = playlistService.upgradePlaylistSimplified(previousTotwPlaylist.get());
      playlistService.clearPlaylist(playlist);
    }

    // Fill playlist with songs
    List<String> songIds = totwData.getTotwEntities().stream()
        .map(TotwEntity.Partial::getSongId)
        .collect(Collectors.toList());
    playlistService.addSongsToPlaylistById(playlist, songIds);

    // Get the tracks as complete Track objects and return LPTarget
    List<Track> allPlaylistTracks = playlistService.getAllPlaylistTracks(playlist);
    return new LPTarget(playlist, allPlaylistTracks, totwData);
  }
}
