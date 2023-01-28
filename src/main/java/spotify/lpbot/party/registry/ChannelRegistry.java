package spotify.lpbot.party.registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.springframework.stereotype.Service;

import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.lp.StandardListeningParty;
import spotify.lpbot.party.service.TotwCreationService;
import spotify.lpbot.party.service.TrackListCreationService;

@Service
public class ChannelRegistry {
  private final TrackListCreationService trackListCreationService;
  private final TotwCreationService totwPlaylistService;

  private final Map<Long, StandardListeningParty> lpInstancesForChannelId;

  public ChannelRegistry(TrackListCreationService trackListCreationService, TotwCreationService totwPlaylistService) {
    this.trackListCreationService = trackListCreationService;
    this.totwPlaylistService = totwPlaylistService;
    this.lpInstancesForChannelId = new ConcurrentHashMap<>();
  }

  public boolean isRegistered(TextChannel channel) {
    return lpInstancesForChannelId.containsKey(channel.getId());
  }

  public Optional<StandardListeningParty> getExistingLPInstance(TextChannel channel, InteractionOriginalResponseUpdater responder) {
    if (isRegistered(channel)) {
      return Optional.of(lpInstancesForChannelId.get(channel.getId()));
    } else {
      sendGenericUnsetError(responder);
      return Optional.empty();
    }
  }

  public void register(TextChannel channel, InteractionOriginalResponseUpdater responder, String potentialUrl) {
    if (!isRegistered(channel) || lpInstancesForChannelId.get(channel.getId()).isOverwritable()) {
      trackListCreationService.verifyUriAndCreateTarget(potentialUrl)
        .ifPresentOrElse(trackList -> {
          StandardListeningParty simpleListeningParty = new StandardListeningParty(channel, trackList);
          lpInstancesForChannelId.put(channel.getId(), simpleListeningParty);
          responder.addEmbed(DiscordUtils.createSimpleEmbed("Listening Party link set! Type `/start` to begin the session")).update();
        }, () -> {
          responder.addEmbed(DiscordUtils.createErrorEmbed("The provided URL is invalid (no Spotify album/playlist detected or malformed formatting)")).update();
        });
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("A Listening Party is currently in progress for this channel. `/stop` it first!")).update();
    }
  }

  public void registerTotw(TextChannel channel, InteractionOriginalResponseUpdater responder, Attachment totwData) {
    if (!isRegistered(channel) || lpInstancesForChannelId.get(channel.getId()).isOverwritable()) {
      totwPlaylistService.parseAttachmentFile(totwData)
        .map(totwPlaylistService::findOrCreateTotwPlaylist)
        .ifPresentOrElse(totwTrackList -> {
          // todo totw party
//          TotwParty totwParty = new TotwParty(channel, responder, totwTrackList);
//          lpInstancesForChannelId.put(channel.getId(), totwParty);
//          responder.addEmbed(DiscordUtils.createSimpleEmbed("Listening Party link set! Type `/start` to begin the session")).update();
        }, () -> {
          responder.addEmbed(DiscordUtils.createErrorEmbed("Failed to create TOTW playlist")).update();
        });
    } else {
      responder.addEmbed(DiscordUtils.createErrorEmbed("A Listening Party is currently in progress for this channel")).update();
    }
  }

  private void sendGenericUnsetError(InteractionOriginalResponseUpdater responder) {
    EmbedBuilder errorEmbed = DiscordUtils.createErrorEmbed("There is currently no Listening Party set for this channel!\n\nUse `/set <link>` to set it");
    responder.addEmbed(errorEmbed).update();
  }
}
