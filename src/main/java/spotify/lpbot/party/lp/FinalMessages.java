package spotify.lpbot.party.lp;

import java.util.List;
import java.util.Random;

public class FinalMessages {
  private static final List<String> FINAL_MESSAGES = List.of(
    "\uD83C\uDFB6 The music has stopped and the party's over. Time to go home and cry in your pillow. Thanks for coming! \uD83D\uDE2D",
    "\uD83C\uDFA7 The Listening Party has officially ended, but don't worry, we'll be back with more sick beats! \uD83D\uDCA5",
    "\uD83C\uDFA4 That's all, folks! Our Listening Party has come to an end. Thanks for laughing and singing with us! \uD83C\uDFB5",
    "\uD83C\uDFB6 It's time to wrap up our Listening Party. Thanks for turning up the volume and turning up the fun! \uD83D\uDD0A",
    "\uD83C\uDFA7 Our DJ is packing up and heading out, so that's the end of our Listening Party. Thanks for shaking what your mama gave you! \uD83D\uDC6F",
    "\uD83C\uDFB5 Our Listening Party has ended, but the beat goes on! Thanks for grooving with us! \uD83D\uDC83",
    "\uD83C\uDFA7 The party's over and the silence is deafening. Thanks for making noise with us! \uD83D\uDE4C",
    "\uD83C\uDFA4 We hope you enjoyed our Listening Party as much as we did. Thanks for singing along and pretending to be a rockstar with us! \uD83E\uDD18",
    "\uD83C\uDFB6 The tunes have stopped and the party's done. Thanks for being a part of our musical family! \uD83C\uDFB5",
    "\uD83C\uDFA7 Our Listening Party is officially over. Thanks for keeping the dance floor hot and our hearts full! \uD83D\uDD25",
    "\uD83C\uDFB5 The end of our Listening Party has come, but the memories will last a lifetime. Thanks for creating them with us! \uD83C\uDF89",
    "\uD83C\uDFA4 That's a wrap on our Listening Party! Thanks for hitting those high notes and making us laugh. You're the real MVP! \uD83C\uDFC6",
    "\uD83C\uDFB6 Our Listening Party has ended, but don't let that stop you from dancing in your dreams! Thanks for joining us! \uD83D\uDCA4",
    "\uD83C\uDFA7 The music has stopped and the crowd has dispersed. Thanks for being the life of our Listening Party! \uD83D\uDC83",
    "\uD83C\uDFB5 We hate to see our Listening Party come to an end, but we love to watch you leave with smiles on your faces. Thanks for being amazing! \uD83D\uDE0A",
    "\uD83C\uDF89 This Listening Party is over. Thank you for joining! \uD83C\uDF8A"
  );

  private static final Random RANDOM_NUMBER_GENERATOR = new Random();

  /**
   * Return a random message for when the listening Party has ended.
   *
   * @return the message as String
   */
  public static String getRandomFinalMessage() {
    int i = RANDOM_NUMBER_GENERATOR.nextInt(FINAL_MESSAGES.size());
    return FINAL_MESSAGES.get(i);
  }
}
