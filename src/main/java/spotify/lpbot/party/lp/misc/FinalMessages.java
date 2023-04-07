package spotify.lpbot.party.lp.misc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class FinalMessages {
  private static final List<String> MUSICAL_EMOJIS = List.of(
    "\uD83C\uDFB5", // musical notes
    "\uD83C\uDFB6", // multiple musical notes
    "\uD83C\uDFBC", // staff with treble clef
    "\uD83C\uDFB9", // piano roll
    "\uD83C\uDFB8", // guitar
    "\uD83C\uDFA4", // microphone
    "\uD83C\uDFA7"  // headphones
  );

  private static final String FINAL_MESSAGES_FILE = "spotify/lpbot/party/lp/misc/final_messages.txt";

  private final Random random;
  private final List<String> finalMessages;

  FinalMessages() {
    this.random = new Random();
    this.finalMessages = readFinalMessagesFile();
  }

  /**
   * Return a random message for when the listening Party has ended.
   *
   * @return the message as String
   */
  public String getRandomFinalMessage() {
    String leadingEmoji = randomEntryFromArray(MUSICAL_EMOJIS);
    String trailingEmoji = randomEntryFromArray(MUSICAL_EMOJIS);
    String finalMessage = randomEntryFromArray(finalMessages);
    return String.format("%s %s %s", leadingEmoji, finalMessage, trailingEmoji);
  }

  private String randomEntryFromArray(List<String> arr) {
    return arr.get(random.nextInt(arr.size()));
  }

  private List<String> readFinalMessagesFile() {
    InputStream resource = getClass().getClassLoader().getResourceAsStream(FINAL_MESSAGES_FILE);
    return new BufferedReader(new InputStreamReader(Objects.requireNonNull(resource)))
      .lines()
      .filter(line -> !line.isBlank())
      .map(String::strip)
      .collect(Collectors.toList());
  }
}
