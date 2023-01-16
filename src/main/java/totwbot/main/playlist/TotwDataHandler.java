package totwbot.main.playlist;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TotwDataHandler {
  private static final String TOTW_DATA_FILE = "./totwdata.txt";

  private final List<TotwEntity> totwEntityList;

  private TotwDataHandler() {
    List<String> totwData = parseTotwDataFile();
    int blockSize = Integer.parseInt(totwData.remove(0));

    List<TotwEntity> totwEntityList1 = new ArrayList<>();
    List<TotwEntity> totwEntityList2 = new ArrayList<>();
    for (int i = 0; i < blockSize; i++) {
      String name = totwData.get(i);
      String lastFmName = totwData.get(i + (blockSize));

      String spotifyLink1 = totwData.get(i + (blockSize * 2));
      String writeUp1 = totwData.get(i + (blockSize * 3));
      TotwEntity totwEntity1 = new TotwEntity(name, lastFmName, spotifyLink1, writeUp1);
      totwEntityList1.add(totwEntity1);

      String spotifyLink2 = totwData.get(i + (blockSize * 4));
      String writeUp2 = totwData.get(i + (blockSize * 5));
      TotwEntity totwEntity2 = new TotwEntity(name, lastFmName, spotifyLink2, writeUp2);
      totwEntityList2.add(totwEntity2);
    }

    totwEntityList = new ArrayList<>();
    totwEntityList.addAll(totwEntityList1);
    totwEntityList.addAll(totwEntityList2);
  }

  public List<TotwEntity> getTotwEntityList() {
    return totwEntityList;
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
