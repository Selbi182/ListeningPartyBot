package lpbot.main.party;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.javacord.api.entity.channel.TextChannel;
import org.springframework.stereotype.Component;

@Component
public class LPChannelRegistry {
  private final Map<TextChannel, LPInstance> lpInstancesForChannel;

  public LPChannelRegistry() {
    this.lpInstancesForChannel = new ConcurrentHashMap<>();
  }

  public LPInstance getLPInstance(TextChannel textChannel) {
    if (isRegistered(textChannel)) {
      return lpInstancesForChannel.get(textChannel);
    } else {
      return register(textChannel);
    }
  }

  public boolean isRegistered(TextChannel textChannel) {
    return lpInstancesForChannel.containsKey(textChannel);
  }

  private LPInstance register(TextChannel textChannel) {
    LPInstance lpInstance = new LPInstance();
    lpInstancesForChannel.put(textChannel, lpInstance);
    return lpInstance;
  }
}
