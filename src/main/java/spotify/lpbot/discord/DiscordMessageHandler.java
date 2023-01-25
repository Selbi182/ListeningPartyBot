package spotify.lpbot.discord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import spotify.lpbot.discord.util.SpamProtector;
import spotify.lpbot.party.data.LPInstance;
import spotify.lpbot.party.service.ChannelRegistry;
import spotify.lpbot.party.totw.TotwDataHandler;
import spotify.lpbot.party.totw.TotwEntity;

@Component
public class DiscordMessageHandler {
  private final ChannelRegistry lpChannelRegistry;

  public DiscordMessageHandler(ChannelRegistry lpChannelRegistry) {
     this.lpChannelRegistry = lpChannelRegistry;
  }

  public void processSlashCommand(SlashCommandCreateEvent slashCommandCreateEvent) {
    User user = slashCommandCreateEvent.getSlashCommandInteraction().getUser();
    if (SpamProtector.checkAuthorOkay(user)) {
      slashCommandCreateEvent.getSlashCommandInteraction().getChannel()
        .ifPresent(channel -> {
          Optional<LPInstance> lpInstance = lpChannelRegistry.getExistingLPInstance(channel);
          InteractionImmediateResponseBuilder responder = slashCommandCreateEvent.getSlashCommandInteraction().createImmediateResponder();
          SlashCommandInteraction slashCommandInteraction = slashCommandCreateEvent.getSlashCommandInteraction();
          String command = slashCommandInteraction.getFullCommandName();
          switch (command) {
            case "start":
              start(responder, lpInstance, slashCommandInteraction.getOptionByName("countdown"));
              break;
            case "stop":
              stop(responder, lpInstance);
              break;
            case "status":
              status(responder, lpInstance);
              break;
            case "set":
              set(responder, channel, slashCommandInteraction.getOptionByName("url"));
              break;
            case "link":
              link(responder, lpInstance);
              break;
            case "totw":
              totw(responder, channel, slashCommandCreateEvent.getSlashCommandInteraction());
              break;
            case "help":
              help(responder, channel);
              break;
            default:
              sendBasicUsageEmbed(responder, channel);
              break;
          }
        });
    }
  }

  //////////////////////

  private void start(InteractionImmediateResponseBuilder responder, Optional<LPInstance> lpInstance, Optional<SlashCommandInteractionOption> customCountdown) {
    int countdown = Math.toIntExact(customCountdown
      .map(SlashCommandInteractionOption::getLongValue)
      .map(Optional::get)
      .orElse(5L));

    try {
      if (countdown < 1) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      DiscordUtils.sendResponse(responder, countdown + " isn't a valid positive number");
      return;
    }
    lpInstance.ifPresentOrElse(lp -> lp.start(responder, countdown), () -> sendGenericUnsetError(responder));
  }

  private void stop(InteractionImmediateResponseBuilder responder, Optional<LPInstance> lpInstance) {
    lpInstance.ifPresentOrElse(lp -> lp.stop(responder), () -> sendGenericUnsetError(responder));
  }

  private void status(InteractionImmediateResponseBuilder responder, Optional<LPInstance> lpInstance) {
    lpInstance.ifPresentOrElse(lp -> lp.status(responder), () -> sendGenericUnsetError(responder));
  }

  private void set(InteractionImmediateResponseBuilder responder, TextChannel channel, Optional<SlashCommandInteractionOption> linkFromCommand) {
    linkFromCommand
      .map(SlashCommandInteractionOption::getStringValue)
      .map(Optional::get)
      .ifPresent(link -> {
        LPInstance registeredInstance = lpChannelRegistry.register(channel, link);
        if (registeredInstance != null) {
          DiscordUtils.sendResponse(responder, "Listening Party link set! Use `/start` to begin the session");
        } else {
          DiscordUtils.sendResponse(responder, "ERROR: Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
        }
      });
  }

  private void link(InteractionImmediateResponseBuilder responder, Optional<LPInstance> lpInstance) {
    lpInstance.ifPresentOrElse((lp) -> DiscordUtils.sendResponse(responder, lp.getAlbumOrPlaylistUri()), () -> sendGenericUnsetError(responder));
  }

  private void totw(InteractionImmediateResponseBuilder responder, TextChannel channel, SlashCommandInteraction slashCommandInteraction) {
    // TODO for now only I got the power lol
    final long SELBI_ID = 186507215807447041L;
    if (slashCommandInteraction.getUser().getId() == SELBI_ID) {
      slashCommandInteraction.getArgumentAttachmentValueByName("attachment").ifPresent(attachment -> {
        if (!attachment.isImage()) {
          try {
            String fileContent = StreamUtils.copyToString(attachment.getUrl().openStream(), StandardCharsets.UTF_8);
            TotwEntity totwData = TotwDataHandler.parseTextFile(fileContent);
            LPInstance registeredInstance = lpChannelRegistry.registerTotw(channel, totwData);
            if (registeredInstance != null) {
              responder.setContent("**TOTW party is set (" + totwData.getParticipants() + " participants)! Use `/start` to begin the session**\n" + registeredInstance.getAlbumOrPlaylistUri()).respond();
            } else {
              DiscordUtils.sendResponse(responder, "ERROR: Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
            }
          } catch (IOException | RuntimeException e) {
            DiscordUtils.sendResponse(responder, "Invalid TOTW data format.");
            e.printStackTrace();
          }
        }
      });
    } else {
      DiscordUtils.sendResponse(responder, "Access denied.");
    }
  }

  private void help(InteractionImmediateResponseBuilder responder, TextChannel channel) {
    responder.setContent("Help").respond();

    EmbedBuilder helpEmbed = new EmbedBuilder();
    helpEmbed.setTitle("Listening Party Bot - Help");
    helpEmbed.addField("`/start <countdown>`", "Start the Listening Party (`countdown` can be set manually in seconds, defaults to 5)");
    helpEmbed.addField("`/status`", " While a Listening Party is ongoing, prints the current song with timestamp (so that late joiners aren't left behind)");
    helpEmbed.addField("`/stop`", "Cancel a currently ongoing Listening Party and reset it to the beginning");
    helpEmbed.addField("`/link`", "Print the set album/playlist link");
    helpEmbed.addField("`/set`", " Set the album/playlist link (must be provided as URL)");
    helpEmbed.addField("`/help`", "Display this page");
    helpEmbed.addField("`/totw`", "[Restricted Access] Host a Track-of-the-Week party. This feature is only available to trusted users");
    channel.sendMessage(helpEmbed);
  }

  private void sendBasicUsageEmbed(InteractionImmediateResponseBuilder responder, TextChannel channel) {
    responder.setContent("Basic Usage").respond();

    EmbedBuilder basicUsageEmbed = new EmbedBuilder();
    basicUsageEmbed.setDescription("`/<command>`\n\nSee `/help` for more information");
    channel.sendMessage(basicUsageEmbed);
  }

  private void sendGenericUnsetError(InteractionImmediateResponseBuilder responder) {
    DiscordUtils.sendResponse(responder, "There's currently no Listening Party set for this channel! Use `/set <link>` to set it");
  }
}
