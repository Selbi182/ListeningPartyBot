package spotify.lpbot.party.service;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.javacord.api.entity.Attachment;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.TotwData;
import spotify.lpbot.party.data.tracklist.TotwTrackListWrapper;
import spotify.services.PlaylistService;
import spotify.util.BotUtils;

@Service
public class TotwCreationService {
  private static final int MAX_ATTACHMENT_SIZE_IN_BYTES = 50 * 1024;

  private final PlaylistService playlistService;
  private final ColorService colorService;
  private final LastFmService lastFmService;

  TotwCreationService(PlaylistService playlistService, ColorService colorService, LastFmService lastFmService) {
    this.playlistService = playlistService;
    this.colorService = colorService;
    this.lastFmService = lastFmService;
  }

  public TotwData parseAttachmentFile(Attachment attachment) throws IOException {
    if (attachment.getSize() > MAX_ATTACHMENT_SIZE_IN_BYTES) {
      throw new IllegalArgumentException("File is too big");
    }

    String fileContent = StreamUtils.copyToString(attachment.getUrl().openStream(), StandardCharsets.UTF_8);
    JsonObject json = JsonParser.parseString(fileContent).getAsJsonObject();

    String headline = json.get("title").getAsString();
    JsonArray jsonSubmissions = json.get("submissions").getAsJsonArray();

    List<TotwData.Entry> fullSubmissions = new ArrayList<>();
    for (JsonElement element : jsonSubmissions) {
      JsonObject entry = element.getAsJsonObject();
      String name = entry.get("name").getAsString();
      String lastFmName = entry.get("lastFmName").getAsString();
      String link = entry.get("link").getAsString();
      String writeUp = entry.get("writeUp").getAsString().replaceAll("\\n", "\n");
      TotwData.Entry totwEntry = new TotwData.Entry(name, lastFmName, link, writeUp);
      lastFmService.attachLastFmData(totwEntry);
      fullSubmissions.add(totwEntry);
    }

    return new TotwData(headline, fullSubmissions);
  }

  public TotwTrackListWrapper findOrCreateTotwPlaylist(TotwData totwData) {
    // Check if a playlist already exists
    List<PlaylistSimplified> userPlaylists = playlistService.getCurrentUsersPlaylists();
    Optional<PlaylistSimplified> previousTotwPlaylist = userPlaylists.stream()
        .filter(p -> p.getName().equals(totwData.getHeadline()))
        .findFirst();

    // If it doesn't exist, create it. If it does, clear it
    Playlist playlist;
    if (previousTotwPlaylist.isEmpty()) {
      playlist = playlistService.createPlaylist(totwData.getHeadline());
    } else {
      playlist = playlistService.upgradePlaylistSimplified(previousTotwPlaylist.get());
      playlistService.clearPlaylist(playlist);
    }

    // Fill playlist with songs
    List<String> songIds = totwData.getTotwEntries().stream()
        .map(TotwData.Entry::getSongId)
        .collect(Collectors.toList());
    playlistService.addSongsToPlaylistById(playlist, songIds);

    // Get the tracks as complete Track objects
    List<Track> allPlaylistTracks = playlistService.getAllPlaylistTracks(playlist);

    // Find the colors for each track
    List<Color> colors = allPlaylistTracks.stream()
        .map(Track::getAlbum)
        .map(AlbumSimplified::getImages)
        .map(BotUtils::findLargestImage)
        .map(colorService::getDominantColorFromImage)
        .collect(Collectors.toList());

    return new TotwTrackListWrapper(playlist, allPlaylistTracks, colors, totwData);
  }
}
