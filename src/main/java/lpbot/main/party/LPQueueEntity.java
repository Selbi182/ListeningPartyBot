package lpbot.main.party;

public class LPQueueEntity {
  private final Runnable runnable;
  private final long nextDelay;

  public LPQueueEntity(Runnable runnable, long nextDelay) {
    this.runnable = runnable;
    this.nextDelay = nextDelay;
  }

  public static LPQueueEntity of(Runnable runnable, long nextDelay) {
    return new LPQueueEntity(runnable, nextDelay);
  }

  public Runnable getRunnable() {
    return runnable;
  }

  public long getNextDelay() {
    return nextDelay;
  }
}
