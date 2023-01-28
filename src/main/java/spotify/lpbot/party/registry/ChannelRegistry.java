package spotify.lpbot.party.registry;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.springframework.stereotype.Service;

import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.TotwData;
import spotify.lpbot.party.data.tracklist.TotwTrackListWrapper;
import spotify.lpbot.party.lp.AbstractListeningParty;
import spotify.lpbot.party.lp.StandardListeningParty;
import spotify.lpbot.party.lp.TotwListeningParty;
import spotify.lpbot.party.service.TotwCreationService;
import spotify.lpbot.party.service.TrackListCreationService;

@Service
public class ChannelRegistry {
  private final TrackListCreationService trackListCreationService;
  private final TotwCreationService totwPlaylistService;

  private final Map<Long, AbstractListeningParty> lpInstancesForChannelId;

  ChannelRegistry(TrackListCreationService trackListCreationService, TotwCreationService totwPlaylistService) {
    this.trackListCreationService = trackListCreationService;
    this.totwPlaylistService = totwPlaylistService;
    this.lpInstancesForChannelId = new ConcurrentHashMap<>();
  }

  public boolean isRegistered(TextChannel channel) {
    return lpInstancesForChannelId.containsKey(channel.getId());
  }

  public Optional<AbstractListeningParty> getExistingLPInstance(TextChannel channel, InteractionOriginalResponseUpdater responder) {
    if (isRegistered(channel)) {
      return Optional.of(lpInstancesForChannelId.get(channel.getId()));
    } else {
      sendGenericUnsetError(responder);
      return Optional.empty();
    }
  }

  public void register(TextChannel channel, InteractionOriginalResponseUpdater responder, String potentialUrl) {
    if (!isRegistered(channel) || lpInstancesForChannelId.get(channel.getId()).isOverwritable()) {
      trackListCreationService.verifyUriAndCreateTarget(potentialUrl) // todo limit at 200 tracks or 10h
        .ifPresentOrElse(trackListWrapper -> {
          StandardListeningParty simpleListeningParty = new StandardListeningParty(channel, trackListWrapper);
          lpInstancesForChannelId.put(channel.getId(), simpleListeningParty);
          DiscordUtils.updateWithSimpleEmbed(responder, "Listening Party link set! Type `/start` to begin the session");
        },
        () -> DiscordUtils.updateWithErrorEmbed(responder, "The provided URL is invalid (no Spotify album/playlist detected or malformed formatting)"));
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "A Listening Party is currently in progress for this channel. `/stop` it first!");
    }
  }

  public void registerTotw(TextChannel channel, InteractionOriginalResponseUpdater responder, Attachment totwData) {
    if (!isRegistered(channel) || lpInstancesForChannelId.get(channel.getId()).isOverwritable()) {
      try {
        TotwData parsedTotwData = totwPlaylistService.parseAttachmentFile(totwData);
        TotwTrackListWrapper totwTrackListWrapper = totwPlaylistService.findOrCreateTotwPlaylist(parsedTotwData);
        TotwListeningParty totwParty = new TotwListeningParty(channel, totwTrackListWrapper);
        lpInstancesForChannelId.put(channel.getId(), totwParty);
        String participants = String.join(", ", parsedTotwData.getParticipants());
        DiscordUtils.updateWithSimpleEmbed(responder, "TOTW party is set! Use `/start` to begin the session\n\n**Participants:**\n" + participants);
      } catch (IOException e) {
        e.printStackTrace();
        DiscordUtils.updateWithErrorEmbed(responder, "Couldn't parse TOTW data or failed to create/refresh TOTW playlist. Likely caused by an unknown last.fm username or malformed JSON");
      }
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "A Listening Party is currently in progress for this channel");
    }
  }

  private void sendGenericUnsetError(InteractionOriginalResponseUpdater responder) {
    DiscordUtils.updateWithErrorEmbed(responder, "There is currently no Listening Party set for this channel!\n\nUse `/set <link>` to set it");
  }
}
