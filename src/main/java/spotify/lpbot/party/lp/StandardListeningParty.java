package spotify.lpbot.party.lp;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

import org.javacord.api.entity.Icon;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import se.michaelthelin.spotify.model_objects.specification.Track;
import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.lastfm.LastFmTrack;
import spotify.lpbot.party.data.lastfm.LastFmWikiEntry;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.lpbot.party.lp.misc.FinalMessages;
import spotify.lpbot.party.service.LastFmService;
import spotify.util.SpotifyUtils;

public class StandardListeningParty extends AbstractListeningParty {
  private final LastFmService lastFmService;

  public StandardListeningParty(TextChannel channel, TrackListWrapper trackListWrapper, LastFmService lastFmService, FinalMessages finalMessages) {
    super(channel, trackListWrapper, finalMessages);
    this.lastFmService = lastFmService;
  }

  @Override
  protected EmbedBuilder createDiscordEmbedForTrack(Track track) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();

    // Author
    Optional<ServerChannel> serverChannel = getChannel().asServerChannel();
    if (serverChannel.isPresent()) {
      Server server = serverChannel.get().getServer();
      String serverName = server.getName();
      Optional<Icon> serverIcon = server.getIcon();
      String lpTitle = "Listening Party on: " + serverName;
      if (serverIcon.isPresent()) {
        embed.setAuthor(lpTitle, null, serverIcon.get());
      } else {
        embed.setAuthor(lpTitle);
      }
    }

    // [Artist] - [Title]
    String songArtists = SpotifyUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s \u2013 %s", songArtists, songTitle));

    String songLfmLink = track.getExternalUrls().get("spotify");
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Description
    LastFmTrack lastFmTrack = lastFmService.getLastFmTrackInfo(track);
    if (lastFmTrack != null && lastFmTrack.hasWiki()) {
      LastFmWikiEntry wiki = lastFmTrack.getWiki();
      String wikiText = wiki.getSummary().split("<a href")[0];
      String description = DiscordUtils.formatDescription(String.format("From [last.fm](%s) (%s)", lastFmTrack.getUrl(), wiki.getFormattedPublishDate()), wikiText);
      embed.setDescription(description);
    }

    // Field info
    String songOrdinal = getCurrentTrackNumber() + " of " + getTotalTrackCount();
    embed.addField("Track Number:", songOrdinal, true);

    String songLength = SpotifyUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    if (lastFmTrack != null && lastFmTrack.hasScrobbles()) {
      String globalScrobbles = NumberFormat.getInstance(Locale.US).format(lastFmTrack.getScrobbleCount());
      embed.addField("Global Scrobbles:", globalScrobbles, true);
    }

    // Image and color
    String imageUrl = SpotifyUtils.findLargestImage(track.getAlbum().getImages());
    embed.setImage(imageUrl);

    Color embedColor = getColorForCurrentTrack();
    embed.setColor(embedColor);

    // "Album: [Artist] - [Album] ([Release year])
    String albumArtists = SpotifyUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = SpotifyUtils.findReleaseYear(track);
    String releaseType = track.getAlbum().getAlbumType().toString();
    embed.setFooter(String.format("%s: %s \u2013 %s (%s)", releaseType, albumArtists, albumName, albumReleaseYear));

    // Send off the embed to the Discord channel
    return embed;
  }
}
