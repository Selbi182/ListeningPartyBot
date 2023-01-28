package spotify.lpbot.party.lp;

import org.springframework.stereotype.Component;

@Component
public class TotwParty  {
/*
  public TotwParty(TextChannel channel, InteractionOriginalResponseUpdater responder, Optional<TotwTrackListWrapper> totwTrackList) {
    super();
  }

  @Override
  protected Runnable createTrackRunnable(TextChannel textChannel, PlaylistTrackListWrapper target, int index) {
    return () -> {
      Track t = target.getTracks().get(index);
      int currentTrackNumber = index + 1;
      String image = BotUtils.findLargestImage(t.getAlbum().getImages());
      NowPlayingInfo currentTrack = new NowPlayingInfo(t, currentTrackNumber, target.getTracks().size(), image);
      TotwData.Entry totwEntity = target.getTotwData().getTotwEntities().get(index);
      TotwData.Full totwEntityFull = upgradeTotwEntity(totwEntity);
      EmbedBuilder discordEmbedForTrack = createDiscordEmbedForTotwEntry(currentTrack, totwEntityFull);
      textChannel.sendMessage(discordEmbedForTrack);
      putCurrentTrack(textChannel.getId(), currentTrack);
    };
  }

  private TotwData.Full upgradeTotwEntity(TotwData.Entry totwEntity) {
    return lastFmDataHandler.attachLastFmData(totwEntity);
  }

  private EmbedBuilder createDiscordEmbedForTotwEntry(NowPlayingInfo nowPlayingInfo, TotwData.Full totwEntity) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();
    Track track = nowPlayingInfo.getTrack();

    // (n/m) Subbed by: [entered name]
    // -> link to last.fm profile
    // -> with last.fm pfp
    String subbedBy = totwEntity.getName();
    String lfmProfileLink = totwEntity.getUserPageUrl();
    String lfmProfilePicture = totwEntity.getProfilePictureUrl();
    String songOrdinal = nowPlayingInfo.getTrackNumber() + " / " + nowPlayingInfo.getTotalTrackCount();
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

    String songLfmLink = totwEntity.getSongLinkUrl();
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Write-up (with fancy quotation marks and in cursive)
    if (!totwEntity.getWriteUp().isBlank()) {
      String writeUp = Arrays.stream(totwEntity.getWriteUp().split("\n"))
        .map(w -> String.format("> %s", w))
        .collect(Collectors.joining("\n"));
      embed.setDescription(String.format("**Write-up:**\n*\u201C%s\u201D*", writeUp));
    }

    // Field info
    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    Integer scrobbleCount = totwEntity.getScrobbleCount();
    if (scrobbleCount != null && scrobbleCount > 0) {
      embed.addField(subbedBy + "\u2019s Scrobbles:", String.valueOf(scrobbleCount), true);
    }

    // Full-res cover art
    String imageUrl = track.getAlbum().getImages()[0].getUrl();
    Color embedColor = colorProvider.getDominantColorFromImage(imageUrl);
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
  */
}
