package totwbot.main.party;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SingleTaskWaitingExecutor {
  private final Queue<DelayedTask> queue;
  private final ScheduledExecutorService scheduler;
  private final List<ScheduledFuture<?>> futures;

  public SingleTaskWaitingExecutor() {
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
    this.queue = new ConcurrentLinkedQueue<>();
    this.futures = new ArrayList<>();
  }

  public void schedule(Runnable task, long delay) {
    DelayedTask delayedTask = new DelayedTask(task, delay);
    this.queue.add(delayedTask);
  }

  public void start() {
    DelayedTask poll = queue.poll();
    long accDelay = 0;
    while (poll != null) {
      ScheduledFuture<?> future = scheduler.schedule(poll.getTask(), accDelay, TimeUnit.MILLISECONDS);
      futures.add(future);
      accDelay += poll.getDelay();
      poll = queue.poll();
    }
  }

  public void stop() {
    for (ScheduledFuture<?> future : futures) {
      future.cancel(true);
    }
    futures.clear();
  }

  static class DelayedTask {
    private final Runnable task;
    private final long delay;

    public DelayedTask(Runnable task, long delay) {
      this.task = task;
      this.delay = delay;
    }

    public long getDelay() {
      return delay;
    }

    public Runnable getTask() {
      return task;
    }
  }
}
