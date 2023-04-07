package spotify.lpbot.party.lp.misc;

import java.io.BufferedReader;
import java.io.File;
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

  private static final String FINAL_MESSAGES_FILE = "final_messages.txt";

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

    // Trailing emoji should be different to leading emoji, but it shouldn't cause an infinite loop
    String trailingEmoji = null;
    for (int i = 0; i < 10; i++) {
      trailingEmoji = randomEntryFromArray(MUSICAL_EMOJIS);
      if (!trailingEmoji.equals(leadingEmoji)) {
        break;
      }
    }

    String finalMessage = randomEntryFromArray(finalMessages);
    return String.format("%s\u2000%s\u2000%s", leadingEmoji, finalMessage, trailingEmoji);
  }

  private String randomEntryFromArray(List<String> arr) {
    return arr.get(random.nextInt(arr.size()));
  }

  private List<String> readFinalMessagesFile() {
    String packageName = getClass().getPackageName().replaceAll("\\.", "/");
    String finalMessagesFilePath = new File(packageName, FINAL_MESSAGES_FILE).getPath();
    InputStream resource = getClass().getClassLoader().getResourceAsStream(finalMessagesFilePath);
    return new BufferedReader(new InputStreamReader(Objects.requireNonNull(resource)))
      .lines()
      .filter(line -> !line.isBlank())
      .map(String::strip)
      .collect(Collectors.toList());
  }
}
