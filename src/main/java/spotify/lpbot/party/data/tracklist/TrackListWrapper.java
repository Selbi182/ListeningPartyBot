package spotify.lpbot.party.data.tracklist;

import java.awt.Color;
import java.util.List;

import se.michaelthelin.spotify.model_objects.specification.Track;

public interface TrackListWrapper {

  String getLink();

  List<Track> getTracks();

  Color getColorByTrackIndex(int index);
}
