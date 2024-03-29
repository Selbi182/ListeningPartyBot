package spotify.lpbot.party.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.javacord.api.entity.Attachment;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.CustomLpData;
import spotify.lpbot.party.data.color.ColorService;
import spotify.lpbot.party.data.tracklist.CustomLpTrackListWrapper;
import spotify.services.PlaylistService;
import spotify.util.SpotifyUtils;

@Service
public class CustomLpCreationService {
  private static final int MAX_ATTACHMENT_SIZE_IN_BYTES = 50 * 1024;

  private final PlaylistService playlistService;
  private final ColorService colorService;

  CustomLpCreationService(PlaylistService playlistService, ColorService colorService) {
    this.playlistService = playlistService;
    this.colorService = colorService;
  }

  public CustomLpData parseAttachmentFile(Attachment attachment) throws IOException {
    if (attachment.getSize() > MAX_ATTACHMENT_SIZE_IN_BYTES) {
      throw new IllegalArgumentException("File is too big");
    }

    String fileContent = StreamUtils.copyToString(attachment.getUrl().openStream(), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();

    String headline = json.get("title").getAsString();
    JsonArray jsonSubmissions = json.get("submissions").getAsJsonArray();

    List<CustomLpData.Entry> partialSubmissions = new ArrayList<>();
    for (JsonElement element : jsonSubmissions) {
      JsonObject entry = element.getAsJsonObject();
      String name = entry.get("name").getAsString().trim();
      String lastFmName = entry.get("lastFmName").getAsString().trim();
      String link = entry.get("link").getAsString().trim();
      String writeUp = entry.get("writeUp").getAsString().replaceAll("\\n", "\n").trim();
      CustomLpData.Entry customLpDataEntry = new CustomLpData.Entry(name, lastFmName, link, writeUp);
      partialSubmissions.add(customLpDataEntry);
    }

    return new CustomLpData(headline, partialSubmissions);
  }

  public CustomLpTrackListWrapper findOrCreateCustomLpPlaylist(CustomLpData customLpData) {
    // Check if a playlist already exists
    List<PlaylistSimplified> userPlaylists = playlistService.getCurrentUsersPlaylists();
    Optional<PlaylistSimplified> previousCustomLpPlaylist = userPlaylists.stream()
        .filter(p -> p.getName().equals(customLpData.getHeadline()))
        .findFirst();

    // If it doesn't exist, create it. If it does, clear it
    Playlist playlist;
    if (previousCustomLpPlaylist.isEmpty()) {
      playlist = playlistService.createPlaylist(customLpData.getHeadline(), null, true);
    } else {
      playlist = playlistService.upgradePlaylistSimplified(previousCustomLpPlaylist.get());
      playlistService.clearPlaylist(playlist);
    }

    // Fill playlist with songs
    List<String> songIds = customLpData.getCustomLpEntries().stream()
        .map(CustomLpData.Entry::getSpotifyLink)
        .map(link -> {
          try {
            if (SpotifyUtils.isShortSpotifyUrl(link)) {
              link = SpotifyUtils.getFullUrlFromShortSpotifyUrl(link);
            }
            return SpotifyUtils.getIdFromSpotifyUrl(link);
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    playlistService.addSongsToPlaylistById(playlist, songIds);

    // Get the tracks as complete Track objects
    List<Track> allPlaylistTracks = playlistService.getAllPlaylistSongs(playlist);

    // Find the colors for each track
    List<ColorFetchResult.RGB> colors = allPlaylistTracks.stream()
        .map(Track::getAlbum)
        .map(AlbumSimplified::getImages)
        .map(SpotifyUtils::findSmallestImage)
        .map(colorService::getDominantColorFromImageUrl)
        .map(ColorFetchResult::getPrimary)
        .collect(Collectors.toList());

    return new CustomLpTrackListWrapper(playlist, allPlaylistTracks, colors, customLpData);
  }
}
