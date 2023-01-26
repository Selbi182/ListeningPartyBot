package spotify.lpbot.discord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.javacord.api.entity.Attachment;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
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
  private final DiscordCountdownHandler discordCountdownHandler;

  public DiscordMessageHandler(ChannelRegistry lpChannelRegistry, DiscordCountdownHandler discordCountdownHandler) {
     this.lpChannelRegistry = lpChannelRegistry;
     this.discordCountdownHandler = discordCountdownHandler;
  }

  public void processSlashCommand(SlashCommandCreateEvent slashCommandCreateEvent) {
    SlashCommandInteraction slashCommandInteraction = slashCommandCreateEvent.getSlashCommandInteraction();
    User user = slashCommandInteraction.getUser();
    if (SpamProtector.checkAuthorOkay(user)) {
      slashCommandInteraction.getChannel()
        .ifPresent(channel -> {
          InteractionOriginalResponseUpdater responder = slashCommandInteraction.respondLater().join();

          Optional<LPInstance> lpInstance = lpChannelRegistry.getExistingLPInstance(channel);
          String command = slashCommandInteraction.getFullCommandName();

          EmbedBuilder responseEmbed = null;
          switch (command) {
            case "start":
              if (isStartable(lpInstance)) {
                Optional<SlashCommandInteractionOption> customCountdownSeconds = slashCommandInteraction.getOptionByName("countdown");
                Optional<Integer> countdownSeconds = discordCountdownHandler.getCountdownSeconds(customCountdownSeconds);
                if (countdownSeconds.isPresent()) {
                  discordCountdownHandler.createAndStartCountdown(countdownSeconds.get(), responder, () -> start(lpInstance));
                } else {
                  responseEmbed = DiscordUtils.createErrorEmbed("Custom countdown must be between 0-30 seconds");
                }
              } else {
                responseEmbed = DiscordUtils.createErrorEmbed("No link set or Listening Party is already in progress for this channel");
              }
              break;
          case "stop":
              responseEmbed = stop(lpInstance);
              break;
            case "status":
              responseEmbed = status(lpInstance);
              break;
            case "set":
              Optional<SlashCommandInteractionOption> url = slashCommandInteraction.getOptionByName("url");
              responseEmbed = set(channel, url);
              break;
            case "link":
              responseEmbed = link(lpInstance);
              break;
            case "totw":
              Optional<Attachment> totwData = slashCommandInteraction.getArgumentAttachmentValueByName("attachment");
              responseEmbed = totw(channel, totwData);
              break;
            case "help":
              responseEmbed = help();
              break;
            default:
              responseEmbed = sendBasicUsageEmbed();
              break;
          }
          if (responseEmbed != null) {
            responder
              .addEmbed(responseEmbed)
              .update();
          }
        });
    }
  }

  //////////////////////

  private boolean isStartable(Optional<LPInstance> lpInstance) {
    return lpInstance
        .map(lp -> lp.isIdle() && !discordCountdownHandler.isCountingDown(lp))
        .orElse(false);
  }

  private void start(Optional<LPInstance> lpInstance) {
    lpInstance
        .ifPresent(LPInstance::start);
  }

  private EmbedBuilder stop(Optional<LPInstance> lpInstance) {
    return lpInstance
        .map(LPInstance::stop)
        .orElseGet(this::genericUnsetError);
  }

  private EmbedBuilder status(Optional<LPInstance> lpInstance) {
    return lpInstance
        .map(LPInstance::status)
        .orElseGet(this::genericUnsetError);
  }

  private EmbedBuilder set(TextChannel channel, Optional<SlashCommandInteractionOption> linkFromCommand) {
    return linkFromCommand
      .map(SlashCommandInteractionOption::getStringValue)
      .map(Optional::get)
      .map(link -> {
        LPInstance registeredInstance = lpChannelRegistry.register(channel, link);
        if (registeredInstance != null) {
          return DiscordUtils.createSimpleEmbed("Listening Party link set! Use `/start` to begin the session");
        } else {
          return DiscordUtils.createErrorEmbed("Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
        }
      })
      .orElseGet(() -> DiscordUtils.createErrorEmbed("URL to album/playlist required!"));
  }

  private EmbedBuilder link(Optional<LPInstance> lpInstance) {
    return lpInstance
        .map(instance -> DiscordUtils.createSimpleEmbed(instance.getAlbumOrPlaylistUri()))
        .orElseGet(this::genericUnsetError);
  }

  private EmbedBuilder totw(TextChannel channel, Optional<Attachment> optionalAttachment) {
    if (optionalAttachment.isPresent()) {
      Attachment attachment = optionalAttachment.get();
      if (!attachment.isImage()) {
        try {
          String fileContent = StreamUtils.copyToString(attachment.getUrl().openStream(), StandardCharsets.UTF_8);
          TotwEntity totwData = TotwDataHandler.parseTextFile(fileContent);
          LPInstance registeredInstance = lpChannelRegistry.registerTotw(channel, totwData);
          if (registeredInstance != null) {
            return DiscordUtils.createSimpleEmbed("**TOTW party is set (" + totwData.getParticipants().size() + " participants)! Use `/start` to begin the session**\n" + registeredInstance.getAlbumOrPlaylistUri());
          } else {
            return DiscordUtils.createErrorEmbed("Invalid Spotify album/playlist ID or a Listening Party is currently in progress!");
          }
        } catch (IOException | RuntimeException e) {
          e.printStackTrace();
        }
      }
    }
    return DiscordUtils.createErrorEmbed("Invalid TOTW data format.");
  }

  private EmbedBuilder help() {
    EmbedBuilder helpEmbed = new EmbedBuilder();
    helpEmbed.setTitle("Listening Party Bot - Help");
    helpEmbed.addField("`/start <countdown>`", "Start the Listening Party (`countdown` can be set manually in seconds, defaults to 5)");
    helpEmbed.addField("`/status`", " While a Listening Party is ongoing, prints the current song with timestamp (so that late joiners aren't left behind)");
    helpEmbed.addField("`/stop`", "Cancel a currently ongoing Listening Party and reset it to the beginning");
    helpEmbed.addField("`/link`", "Print the set album/playlist link");
    helpEmbed.addField("`/set <url>`", " Set the album/playlist link (`url` being the link to Spotify)");
    helpEmbed.addField("`/help`", "Display this page");
    helpEmbed.addField("`/totw`", "[Restricted Access] Host a Track-of-the-Week party. This feature is only available to trusted users");
    return helpEmbed;
  }

  private EmbedBuilder sendBasicUsageEmbed() {
    EmbedBuilder basicUsageEmbed = new EmbedBuilder();
    basicUsageEmbed.setDescription("`/<command>`\n\nSee `/help` for more information");
    return basicUsageEmbed;
  }

  private EmbedBuilder genericUnsetError() {
    return DiscordUtils.createErrorEmbed("There is currently no Listening Party set for this channel!\n\nUse `/set <link>` to set it");
  }
}
