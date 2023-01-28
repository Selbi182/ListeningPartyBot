package spotify.lpbot.party.lp;

import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.TotwData;
import spotify.lpbot.party.data.tracklist.TotwTrackListWrapper;
import spotify.util.BotUtils;

public class TotwListeningParty extends AbstractListeningParty{
  private final TotwTrackListWrapper totwTrackListWrapper;

  public TotwListeningParty(TextChannel channel, TotwTrackListWrapper totwTrackListWrapper) {
    super(channel, totwTrackListWrapper);
    this.totwTrackListWrapper = totwTrackListWrapper;
  }

  private TotwData.Entry getCurrentTotwEntry() {
    return totwTrackListWrapper.getTotwData().getTotwEntries().get(getCurrentTrackListIndex());
  }

  @Override
  protected EmbedBuilder createDiscordEmbedForTrack(Track track) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();
    TotwData.Entry currentTotwEntry = getCurrentTotwEntry();

    // (n/m) Subbed by: [entered name]
    // -> link to last.fm profile
    // -> with last.fm pfp
    String subbedBy = currentTotwEntry.getName();
    String lfmProfileLink = currentTotwEntry.getUserPageUrl();
    String lfmProfilePicture = currentTotwEntry.getProfilePictureUrl();
    String songOrdinal = getCurrentTrackNumber() + " / " + getTotalTrackCount();
    String authorHeadline = String.format("(%s) Submitted by: %s", songOrdinal, subbedBy);
    if (lfmProfileLink != null && lfmProfilePicture != null) {
      embed.setAuthor(authorHeadline, lfmProfileLink, lfmProfilePicture);
    } else {
      embed.setAuthor(authorHeadline);
    }

    // [Artist] - [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    String songLfmLink = currentTotwEntry.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Write-up (with fancy quotation marks and in cursive)
    if (!currentTotwEntry.getWriteUp().isBlank()) {
      String writeUp = Arrays.stream(currentTotwEntry.getWriteUp().split("\n"))
        .map(String::trim)
        .collect(Collectors.joining("\n> "));
      embed.setDescription(String.format("**Write-up:**\n> *%s*", writeUp));
    }

    // Field info
    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    Integer scrobbleCount = currentTotwEntry.getScrobbleCount();
    if (scrobbleCount != null && scrobbleCount > 0) {
      embed.addField(subbedBy + "\u2019s Scrobbles:", String.valueOf(scrobbleCount), true);
    }

    // Full-res cover art
    String imageUrl = BotUtils.findLargestImage(track.getAlbum().getImages());
    Color embedColor = getColorForCurrentTrack();
    embed.setImage(imageUrl);
    embed.setColor(embedColor);

    // Album: [Artist] - [Album] ([Release year])
    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = BotUtils.findReleaseYear(track);
    embed.setFooter(String.format("Album: %s \u2013 %s (%s)", albumArtists, albumName, albumReleaseYear));

    // Send off the embed to the Discord channel
    return embed;
  }
}
