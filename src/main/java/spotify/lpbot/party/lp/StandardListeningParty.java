package spotify.lpbot.party.lp;

import java.awt.Color;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.util.BotUtils;

public class StandardListeningParty extends AbstractListeningParty {
  public StandardListeningParty(TextChannel channel, TrackListWrapper trackListWrapper) {
    super(channel, trackListWrapper);
  }

  @Override
  protected EmbedBuilder createDiscordEmbedForTrack(Track track) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // "[Artist] - [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    String songLfmLink = track.getExternalUrls().get("spotify");
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Field info
    String songOrdinal = getCurrentTrackNumber() + " of " + getTotalTrackCount();
    embed.addField("Track Number:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    // Image and color
    String imageUrl = BotUtils.findLargestImage(track.getAlbum().getImages());
    embed.setImage(imageUrl);

    Color embedColor = getColorForCurrentTrack();
    embed.setColor(embedColor);

    // "Album: [Artist] - [Album] ([Release year])
    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = BotUtils.findReleaseYear(track);
    embed.setFooter(String.format("%s \u2013 %s (%s)", albumArtists, albumName, albumReleaseYear));

    // Send off the embed to the Discord channel
    return embed;
  }
}
