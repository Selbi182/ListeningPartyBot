package spotify.lpbot.party.handler;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Component;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.lastfm.LastFmDataHandler;
import spotify.lpbot.party.data.LPTarget;
import spotify.lpbot.party.totw.TotwEntity;
import spotify.util.BotUtils;

@Component
public class TotwPartyHandler extends AbstractListeningPartyHandler {
  private final LastFmDataHandler lastFmDataHandler;

  public TotwPartyHandler(LastFmDataHandler lastFmDataHandler) {
    super();
    this.lastFmDataHandler = lastFmDataHandler;
  }

  @Override
  protected Runnable createTrackRunnable(TextChannel textChannel, LPTarget target, int index) {
    return () -> {
      Track t = target.getTracks().get(index);
      int currentTrackNumber = index + 1;
      String image = BotUtils.findLargestImage(t.getAlbum().getImages());
      CurrentTrack currentTrack = new CurrentTrack(t, currentTrackNumber, target.getTracks().size(), image);
      TotwEntity.Partial totwEntity = target.getTotwData().getTotwEntities().get(index);
      TotwEntity.Full totwEntityFull = upgradeTotwEntity(totwEntity);
      EmbedBuilder discordEmbedForTrack = createDiscordEmbedForTotwEntry(currentTrack, totwEntityFull);
      textChannel.sendMessage(discordEmbedForTrack);
      putCurrentTrack(textChannel.getId(), currentTrack);
    };
  }

  private TotwEntity.Full upgradeTotwEntity(TotwEntity.Partial totwEntity) {
    return lastFmDataHandler.attachLastFmData(totwEntity);
  }

  private EmbedBuilder createDiscordEmbedForTotwEntry(CurrentTrack currentTrack, TotwEntity.Full totwEntity) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();
    Track track = currentTrack.getTrack();

    // "Subbed by: [entered name]
    // -> link to last.fm profile
    // -> with last.fm pfp
    String subbedBy = totwEntity.getName();
    String lfmProfileLink = totwEntity.getUserPageUrl();
    String lfmProfilePicture = totwEntity.getProfilePictureUrl();
    String authorText = String.format("Submitted by: %s", subbedBy);
    if (lfmProfileLink != null && lfmProfilePicture != null) {
      embed.setAuthor(authorText, lfmProfileLink, lfmProfilePicture);
    } else {
      embed.setAuthor(authorText);
    }

    // "[Artist] – [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s – %s", songArtists, songTitle));

    String songLfmLink = totwEntity.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Write-up
    if (!totwEntity.getWriteUp().isBlank()) {
      String writeUp = Arrays.stream(totwEntity.getWriteUp().split("<;;;>"))
        .map(s -> "> " + s)
        .collect(Collectors.joining("\n"));
      embed.setDescription("**Write-up:**\n" + writeUp);
    }

    // Field info
    String songOrdinal = currentTrack.getTrackNumber() + " of " + currentTrack.getTotalTrackCount();
    embed.addField("TOTW Entry:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    Integer scrobbleCount = totwEntity.getScrobbleCount();
    if (scrobbleCount != null && scrobbleCount > 0) {
      embed.addField("Total Scrobbles:", String.valueOf(scrobbleCount), true);
    }

    // Full-res cover art
    String imageUrl = track.getAlbum().getImages()[0].getUrl();
    embed.setImage(imageUrl);

    // "Album: [Artist] – [Album] ([Release year])
    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = BotUtils.findReleaseYear(track);
    embed.setFooter(String.format("%s – %s\n(%s)", albumArtists, albumName, albumReleaseYear));

    // Add some finishing touches
    embed.setColor(embedColor);

    // Send off the embed to the Discord channel
    return embed;
  }
}
