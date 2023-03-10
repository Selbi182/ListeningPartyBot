package spotify.lpbot.party.lp;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.TotwData;
import spotify.lpbot.party.data.tracklist.TotwTrackListWrapper;
import spotify.lpbot.party.service.LastFmService;
import spotify.util.SpotifyUtils;

public class TotwListeningParty extends AbstractListeningParty{
  private final TotwTrackListWrapper totwTrackListWrapper;
  private final LastFmService lastFmService;

  public TotwListeningParty(TextChannel channel, TotwTrackListWrapper totwTrackListWrapper, LastFmService lastFmService) {
    super(channel, totwTrackListWrapper);
    this.totwTrackListWrapper = totwTrackListWrapper;
    this.lastFmService = lastFmService;
  }

  private TotwData.Entry getCurrentTotwEntry() {
    return totwTrackListWrapper.getTotwData().getTotwEntries().get(getCurrentTrackListIndex());
  }

  @Override
  protected EmbedBuilder createDiscordEmbedForTrack(Track track) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();
    TotwData.Entry currentTotwEntry = getCurrentTotwEntry();

    // Upgrade last.fm data if possible
    try {
      lastFmService.attachLastFmData(currentTotwEntry);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }

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
    String songArtists = SpotifyUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    String songLfmLink = currentTotwEntry.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Description (song length + Write-up with fancy quotation marks and in cursive)
    String songLength = SpotifyUtils.formatTime(track.getDurationMs());
    String description = "**Song Length:** " + songLength;
    if (!currentTotwEntry.getWriteUp().isBlank()) {
      description += "\n\n" + DiscordUtils.formatDescription("Write-up", currentTotwEntry.getWriteUp());
    }
    embed.setDescription(description);

    // Scrobble info
    Integer scrobbleCount = currentTotwEntry.getScrobbleCount();
    Integer globalScrobbleCount = currentTotwEntry.getGlobalScrobbleCount();
    if (currentTotwEntry.getScrobbleCount() != null && currentTotwEntry.getGlobalScrobbleCount() != null) {
        embed.addField(subbedBy + "\u2019s Scrobbles:", formatNumberWithCommas(scrobbleCount), true);
        embed.addField("Global Scrobbles:", formatNumberWithCommas(globalScrobbleCount), true);
    } else {
      embed.addField("Error:", "Scrobble count couldn't be found for this track");
    }

    // Full-res cover art
    String imageUrl = SpotifyUtils.findLargestImage(track.getAlbum().getImages());
    Color embedColor = getColorForCurrentTrack();
    embed.setImage(imageUrl);
    embed.setColor(embedColor);

    // Album: [Artist] - [Album] ([Release year])
    String albumArtists = SpotifyUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = SpotifyUtils.findReleaseYear(track);
    String releaseType = track.getAlbum().getAlbumType().toString();
    embed.setFooter(String.format("%s: %s \u2013 %s (%s)", releaseType, albumArtists, albumName, albumReleaseYear));

    // Send off the embed to the Discord channel
    return embed;
  }

  private String formatNumberWithCommas(int number) {
    return NumberFormat.getInstance(Locale.US).format(number);
  }
}
