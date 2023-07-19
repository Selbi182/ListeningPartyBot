package spotify.lpbot.party.registry;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.springframework.stereotype.Service;

import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.party.data.CustomLpData;
import spotify.lpbot.party.data.tracklist.CustomLpTrackListWrapper;
import spotify.lpbot.party.data.tracklist.TrackListWrapper;
import spotify.lpbot.party.lp.AbstractListeningParty;
import spotify.lpbot.party.lp.StandardListeningParty;
import spotify.lpbot.party.lp.CustomListeningParty;
import spotify.lpbot.party.lp.misc.FinalMessages;
import spotify.lpbot.party.lp.misc.LpUtils;
import spotify.lpbot.party.service.LastFmService;
import spotify.lpbot.party.service.CustomLpCreationService;
import spotify.lpbot.party.service.TrackListCreationService;

@Service
public class ChannelRegistry {
  private final TrackListCreationService trackListCreationService;
  private final CustomLpCreationService customLpPlaylistService;
  private final LastFmService lastFmService;
  private final FinalMessages finalMessages;
  private final Logger logger;

  private final Map<Long, AbstractListeningParty> lpInstancesForChannelId;

  ChannelRegistry(TrackListCreationService trackListCreationService, CustomLpCreationService customLpPlaylistService, LastFmService lastFmService, FinalMessages finalMessages) {
    this.trackListCreationService = trackListCreationService;
    this.customLpPlaylistService = customLpPlaylistService;
    this.lastFmService = lastFmService;
    this.finalMessages = finalMessages;
    this.logger = Logger.getLogger(ChannelRegistry.class.getName());
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

  public AbstractListeningParty register(TextChannel channel, InteractionOriginalResponseUpdater responder, String potentialUrl, boolean printHint) {
    if (!isRegistered(channel) || lpInstancesForChannelId.get(channel.getId()).isReplaceable()) {
      try {
        TrackListWrapper trackListWrapper = trackListCreationService.verifyUriAndCreateTarget(potentialUrl);
        StandardListeningParty simpleListeningParty = new StandardListeningParty(channel, trackListWrapper, lastFmService, finalMessages);
        lpInstancesForChannelId.put(channel.getId(), simpleListeningParty);
        if (printHint) {
          DiscordUtils.updateWithSimpleEmbed(responder, "Listening Party link set! Type " + DiscordUtils.findClickableCommand("start") + " to begin the session.");
          DiscordUtils.sendSimpleMessage(channel, "**Link:** " + trackListWrapper.getLink());
        }
        LpUtils.logLpEvent(channel, logger, "New LP set up");
        return simpleListeningParty;
      } catch (RuntimeException e) {
        DiscordUtils.updateWithErrorEmbed(responder, e.getMessage());
      }
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "A Listening Party is currently in progress for this channel. " + DiscordUtils.findClickableCommand("stop") + " it first!");
    }
    return null;
  }

  public void registerCustomLp(TextChannel channel, InteractionOriginalResponseUpdater responder, Attachment customLpData, boolean guessingGame, boolean shuffle) {
    if (!isRegistered(channel) || lpInstancesForChannelId.get(channel.getId()).isReplaceable()) {
      try {
        CustomLpData parsedCustomLpData = customLpPlaylistService.parseAttachmentFile(customLpData);
        if (shuffle) {
          parsedCustomLpData.shuffle();
        }
        CustomLpTrackListWrapper customLpTrackListWrapper = customLpPlaylistService.findOrCreateCustomLpPlaylist(parsedCustomLpData);
        CustomListeningParty customListeningParty = new CustomListeningParty(channel, customLpTrackListWrapper, lastFmService, finalMessages, guessingGame);
        lpInstancesForChannelId.put(channel.getId(), customListeningParty);

        String participants = String.join(", ", parsedCustomLpData.getParticipants());
        String customLpInfoEmbed = "Custom party is set! Use " + DiscordUtils.findClickableCommand("start") + " to begin the listening party."
          + "\n\n**Participants (click to reveal names):**\n||" + participants + "||";

        StringJoiner optionsText = new StringJoiner(", ");
        if (shuffle) {
          optionsText.add("Shuffle");
        }
        if (guessingGame) {
          optionsText.add("Guessing Game");
        }
        if (!optionsText.toString().isBlank()) {
          customLpInfoEmbed += "\n\n**Options:** " + optionsText;
        }

        DiscordUtils.updateWithSimpleEmbed(responder, customLpInfoEmbed);

        DiscordUtils.sendSimpleMessage(channel, "**Link:** " + customLpTrackListWrapper.getLink());

        LpUtils.logLpEvent(channel, logger, "New custom LP set up");
      } catch (IOException e) {
        e.printStackTrace();
        DiscordUtils.updateWithErrorEmbed(responder, "Couldn't parse custom data or failed to create/refresh target playlist. Likely caused by an unknown last.fm username or malformed JSON");
      }
    } else {
      DiscordUtils.updateWithErrorEmbed(responder, "A Listening Party is currently in progress for this channel");
    }
  }

  private void sendGenericUnsetError(InteractionOriginalResponseUpdater responder) {
    DiscordUtils.updateWithErrorEmbed(responder, "There is currently no Listening Party set for this channel!\n\nUse " + DiscordUtils.findClickableCommand("set") + " to set it");
  }
}
