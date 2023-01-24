package lpbot.main.party.totw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TotwDataHandler {
  public static TotwEntity parseTextFile(String content) {
    String[] split = content.split(System.lineSeparator());
    List<String> totwData = new ArrayList<>(Arrays.asList(split)); // must be a modifiable list

    String headline = totwData.remove(0);
    int participants = Integer.parseInt(totwData.remove(0));

    List<TotwEntity.Partial> totwEntityList1 = new ArrayList<>();
    List<TotwEntity.Partial> totwEntityList2 = new ArrayList<>();
    for (int i = 0; i < participants; i++) {
      String name = totwData.get(i);
      String lastFmName = totwData.get(i + (participants));

      String spotifyLink1 = totwData.get(i + (participants * 2));
      String writeUp1 = totwData.get(i + (participants * 3));
      TotwEntity.Partial totwEntity1 = new TotwEntity.Partial(name, lastFmName, spotifyLink1, writeUp1, i + 1);
      totwEntityList1.add(totwEntity1);

      // TODO remove the second list and instead make the Google Forms consistent from the start (consistent: one TotwEntity = one song)
      String spotifyLink2 = totwData.get(i + (participants * 4));
      String writeUp2 = totwData.get(i + (participants * 5));
      TotwEntity.Partial totwEntity2 = new TotwEntity.Partial(name, lastFmName, spotifyLink2, writeUp2, i + participants + 1);
      totwEntityList2.add(totwEntity2);
    }
    totwEntityList1.addAll(totwEntityList2);

    return new TotwEntity(headline, participants, totwEntityList1);
  }

}
