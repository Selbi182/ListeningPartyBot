package totwbot.main.discord.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Digester {

  private final Queue<String> wordList;

  public Digester(String rawString) {
    String trimmed = rawString.trim().replaceAll("[ ]+", " ");
    wordList = new LinkedList<>(List.of(trimmed.split(" ")));
  }

  /**
   * Get the next segment of the word list.
   * @return the next segment. null if empty
   */
  public String shift() {
    return wordList.poll();
  }
}
