package lpbot.main.party.impl;

import java.awt.Color;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.springframework.stereotype.Component;

import lpbot.main.party.AbstractListeningPartyHandler;
import lpbot.main.party.data.LPTarget;
import lpbot.main.spotify.util.BotUtils;
import se.michaelthelin.spotify.model_objects.specification.Track;

@Component
public class ListeningPartyHandler extends AbstractListeningPartyHandler {

  @Override
  protected Runnable createTrackRunnable(TextChannel textChannel, LPTarget target, int index) {
    return () -> {
      Track t = target.getTracks().get(index);
      int currentTrackNumber = index + 1;
      String image = BotUtils.findLargestImage(t.getAlbum().getImages());
      CurrentTrack currentTrack = new CurrentTrack(t, currentTrackNumber, target.getTracks().size(), image);
      EmbedBuilder discordEmbedForTrack = createDiscordEmbedForTrack(currentTrack);
      textChannel.sendMessage(discordEmbedForTrack);
      putCurrentTrack(textChannel.getId(), currentTrack);
    };
  }

  private EmbedBuilder createDiscordEmbedForTrack(CurrentTrack currentTrack) {
    // Prepare a new Discord embed
    EmbedBuilder embed = new EmbedBuilder();
    Track track = currentTrack.getTrack();

    // "[Artist] – [Title] ([song:length])
    // -> Link to last.fm page
    String songArtists = BotUtils.joinArtists(track.getArtists());
    String songTitle = track.getName();
    embed.setTitle(String.format("%s – %s", songArtists, songTitle));

    String songLfmLink = track.getExternalUrls().get("spotify");
    if (songLfmLink != null) {
      embed.setUrl(songLfmLink);
    }

    // Field info
    String songOrdinal = currentTrack.getTrackNumber() + " of " + currentTrack.getTotalTrackCount();
    embed.addField("Track Number:", songOrdinal, true);

    String songLength = BotUtils.formatTime(track.getDurationMs());
    embed.addField("Song Length:", songLength, true);

    // Full-res cover art
    embed.setImage(BotUtils.findLargestImage(track.getAlbum().getImages()));

    // "Album: [Artist] – [Album] ([Release year])
    String albumArtists = BotUtils.joinArtists(track.getAlbum().getArtists());
    String albumName = track.getAlbum().getName();
    String albumReleaseYear = BotUtils.findReleaseYear(track);
    embed.setFooter(String.format("%s – %s (%s)", albumArtists, albumName, albumReleaseYear));

    // Add some finishing touches
    Color embedColor = new Color(173, 20, 87); // TODO automate from image (maybe? perhaps overkill)
    embed.setColor(embedColor);

    return embed;
  }
}
