package spotify.lpbot.party.data.tracklist;

import java.util.List;

import de.selbi.colorfetch.data.ColorFetchResult;
import se.michaelthelin.spotify.model_objects.specification.Track;

public interface TrackListWrapper {

  String getLink();

  List<Track> getTracks();

  ColorFetchResult.RGB getColorByTrackIndex(int index);
}
