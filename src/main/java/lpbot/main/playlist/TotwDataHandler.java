package lpbot.main.playlist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TotwDataHandler implements LPDataHandler {
  private static final String TOTW_DATA_FILE = "./totwdata.txt";

  private final List<LPEntity> lpEntityList;

  private TotwDataHandler() {
    List<String> totwData = parseTotwDataFile();
    int blockSize = Integer.parseInt(totwData.remove(0));

    List<LPEntity> totwEntityList1 = new ArrayList<>();
    List<LPEntity> totwEntityList2 = new ArrayList<>();
    for (int i = 0; i < blockSize; i++) {
      String name = totwData.get(i);
      String lastFmName = totwData.get(i + (blockSize));

      String spotifyLink1 = totwData.get(i + (blockSize * 2));
      String writeUp1 = totwData.get(i + (blockSize * 3));
      LPEntity totwEntity1 = new LPEntity(name, lastFmName, spotifyLink1, writeUp1, i + 1);
      totwEntityList1.add(totwEntity1);

      String spotifyLink2 = totwData.get(i + (blockSize * 4));
      String writeUp2 = totwData.get(i + (blockSize * 5));
      LPEntity totwEntity2 = new LPEntity(name, lastFmName, spotifyLink2, writeUp2, i + blockSize + 1);
      totwEntityList2.add(totwEntity2);
    }

    lpEntityList = new ArrayList<>();
    lpEntityList.addAll(totwEntityList1);
    lpEntityList.addAll(totwEntityList2);
  }

  public List<LPEntity> getLPEntityList() {
    return lpEntityList;
  }

  private static List<String> parseTotwDataFile() {
    try {
      File f = new File(TOTW_DATA_FILE);
      List<String> strings = Files.readAllLines(f.toPath());
      return new ArrayList<>(strings); // Must be a modifiable list
    } catch (IOException e) {
      e.printStackTrace();
    }
    return List.of();
  }

}
