package spotify.lpbot.party.lp;

import java.awt.Color;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.TotwData;
import spotify.lpbot.party.data.tracklist.TotwTrackListWrapper;
import spotify.lpbot.party.service.LastFmService;
import spotify.util.BotUtils;

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
    } catch (IOException e) {
      e.printStackTrace();
      DiscordUtils.createErrorEmbed("Failed to attach data from last.fm for this TOTW entry");
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
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    String songLfmLink = currentTotwEntry.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Write-up (with fancy quotation marks and in cursive)
    String songLength = BotUtils.formatTime(track.getDurationMs());
    String description = "**Song Length:** " + songLength;

    if (!currentTotwEntry.getWriteUp().isBlank()) {
      String writeUp = Arrays.stream(currentTotwEntry.getWriteUp().split("\n"))
        .map(String::trim)
        .collect(Collectors.joining("\n> "));
      embed.setDescription(description + "\n\n" + String.format("**Write-up:**\n> *%s*", writeUp));
    }

    // Scrobble info
    Integer scrobbleCount = currentTotwEntry.getScrobbleCount();
    if (scrobbleCount != null && scrobbleCount > 0) {
      embed.addField(subbedBy + "\u2019s Scrobbles:", formatNumberWithCommas(scrobbleCount), true);
    }

    Integer globalScrobbleCount = currentTotwEntry.getGlobalScrobbleCount();
    if (globalScrobbleCount != null && globalScrobbleCount > 0) {
      embed.addField("Global Scrobbles:", formatNumberWithCommas(globalScrobbleCount), true);
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

  private String formatNumberWithCommas(int number) {
    return NumberFormat.getInstance(Locale.US).format(number);
  }
}
