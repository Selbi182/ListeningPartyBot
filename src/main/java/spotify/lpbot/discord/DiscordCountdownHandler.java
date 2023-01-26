package spotify.lpbot.discord;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.springframework.stereotype.Component;

import spotify.lpbot.party.data.LPInstance;

@Component
public class DiscordCountdownHandler {
  private static final long DEFAULT_COUNTDOWN_SECONDS = 5L;
  private static final long MAX_COUNTDOWNS_SECONDS = 30L;
  private static final int COUNTDOWN_INTERVAL_MS = 1000;

  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<Long, ScheduledFuture<?>> countdownFutures;

  DiscordCountdownHandler() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    this.countdownFutures = new ConcurrentHashMap<>();
  }

  public Optional<Integer> getCountdownSeconds(Optional<SlashCommandInteractionOption> customCountdownSeconds) {
    int countdown = Math.toIntExact(customCountdownSeconds
        .map(SlashCommandInteractionOption::getLongValue)
        .map(Optional::get)
        .orElse(DEFAULT_COUNTDOWN_SECONDS));

    if (countdown >= 0 && countdown <= MAX_COUNTDOWNS_SECONDS) {
      return Optional.of(countdown);
    } else {
      return Optional.empty();
    }
  }

  public void createAndStartCountdown(int countdown, InteractionOriginalResponseUpdater responder, Runnable actualLpRunnable) {
    EmbedBuilder countdownEmbed = DiscordUtils.createSimpleEmbed("The Listening Party begins in...", true);

    Message msg = responder.addEmbed(countdownEmbed).update().join();
    TextChannel channel = msg.getChannel();

    AtomicInteger atomicCountdown = new AtomicInteger(countdown);
    ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(() -> {
      int i = atomicCountdown.getAndDecrement();
      StringBuilder description = new StringBuilder();
      for (int j = countdown; j >= i; j--) {
        description.append(j > 0 ? j + "... " : "\uD83C\uDF89 NOW \uD83C\uDF8A");
      }
      countdownEmbed.setDescription(description.toString());
      msg.createUpdater().addEmbed(countdownEmbed).replaceMessage().join();
      if (i <= 0) {
        countdownFutures.remove(channel.getId()).cancel(true);
        scheduledExecutorService.execute(actualLpRunnable);
      }
    }, COUNTDOWN_INTERVAL_MS, COUNTDOWN_INTERVAL_MS, TimeUnit.MILLISECONDS);
    countdownFutures.put(channel.getId(), future);
  }

  public boolean isCountingDown(LPInstance lp) {
    return countdownFutures.containsKey(lp.getTextChannel().getId());
  }
}