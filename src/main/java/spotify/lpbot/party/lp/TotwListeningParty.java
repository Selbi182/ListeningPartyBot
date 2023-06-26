package spotify.lpbot.party.lp;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.TotwData;
import spotify.lpbot.party.data.lastfm.LastFmTrack;
import spotify.lpbot.party.data.lastfm.LastFmUser;
import spotify.lpbot.party.data.tracklist.TotwTrackListWrapper;
import spotify.lpbot.party.lp.misc.FinalMessages;
import spotify.lpbot.party.service.LastFmService;
import spotify.util.SpotifyUtils;

public class TotwListeningParty extends AbstractListeningParty {
  private final TotwTrackListWrapper totwTrackListWrapper;
  private final LastFmService lastFmService;
  private final boolean guessingGame;

  public TotwListeningParty(TextChannel channel, TotwTrackListWrapper totwTrackListWrapper, LastFmService lastFmService, FinalMessages finalMessages, boolean guessingGame) {
    super(channel, totwTrackListWrapper, finalMessages);
    this.totwTrackListWrapper = totwTrackListWrapper;
    this.lastFmService = lastFmService;
    this.guessingGame = guessingGame;
  }

  private TotwData.Entry getCurrentTotwEntry() {
    return totwTrackListWrapper.getTotwData().getTotwEntries().get(getCurrentTrackListIndex());
  }

  @Override
  protected EmbedBuilder createDiscordEmbedForTrack(Track track) {
    if (!guessingGame) {
      return fullTotwEmbed(track);
    }
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // [Artist] - [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = SpotifyUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    // Description
    TotwData.Entry currentTotwEntry = getCurrentTotwEntry();
    String realSubber = currentTotwEntry.getName();

    List<String> participants = new ArrayList<>(totwTrackListWrapper.getTotwData().getParticipants());
    participants.remove(realSubber);
    Collections.shuffle(participants);

    List<String> subberOptions = new ArrayList<>(participants.subList(0, 3));
    subberOptions.add(0, realSubber);
    Collections.shuffle(subberOptions);

    String abcd = String.format("Who subbed this song? \uD83E\uDD14"
        + "\n\n"
        + "> **A** \u2013 %s\n"
        + "> **B** \u2013 %s\n"
        + "> **C** \u2013 %s\n"
        + "> **D** \u2013 %s",
      subberOptions.get(0), subberOptions.get(1), subberOptions.get(2), subberOptions.get(3));
    embed.setDescription(abcd);

    // Thumbnail cover art
    String imageUrl = SpotifyUtils.findLargestImage(track.getAlbum().getImages());
    Color embedColor = getColorForCurrentTrack();
    embed.setThumbnail(imageUrl);
    embed.setColor(embedColor);

    // Footer
    embed.setFooter("Answer gets revealed in 30 seconds...");

    // Send full embed in 30 seconds
    getScheduledExecutorService().schedule(() -> {
      getChannel().sendMessage(fullTotwEmbed(track));
    }, 30, TimeUnit.SECONDS);

    // Send off the embed to the Discord channel
    return embed;
  }

  @Override
  protected void sendEmbed(EmbedBuilder discordEmbedForTrack) {
    if (!guessingGame) {
      super.sendEmbed(discordEmbedForTrack);
      return;
    }
    Message guessingGameMessage = getChannel().sendMessage(discordEmbedForTrack).join();
    guessingGameMessage.addReactions("\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9");
  }

  protected EmbedBuilder fullTotwEmbed(Track track) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();
    TotwData.Entry currentTotwEntry = getCurrentTotwEntry();

    // Upgrade last.fm data if possible
    try {
      String lastFmName = currentTotwEntry.getLastFmName();
      LastFmUser lastFmUserInfo = lastFmService.getLastFmUserInfo(lastFmName);
      if (lastFmUserInfo != null) {
        currentTotwEntry.attachUserInfo(lastFmUserInfo);
      }
      LastFmTrack lastFmTrack = lastFmService.getLastFmTrackInfo(track, lastFmName);
      if (lastFmTrack != null) {
        currentTotwEntry.attachTrackInfo(lastFmTrack);
      }
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
    attachFooter(track, embed);

    // Send off the embed to the Discord channel
    return embed;
  }

  private String formatNumberWithCommas(int number) {
    return NumberFormat.getInstance(Locale.US).format(number);
  }
}
