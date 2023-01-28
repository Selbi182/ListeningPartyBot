package spotify.lpbot.discord;

import java.util.Optional;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.callback.InteractionOriginalResponseUpdater;
import org.springframework.stereotype.Component;

import spotify.lpbot.discord.util.DiscordUtils;
import spotify.lpbot.discord.util.SpamProtector;
import spotify.lpbot.party.lp.StandardListeningParty;
import spotify.lpbot.party.registry.ChannelRegistry;

@Component
public class DiscordMessageHandler {
  private final ChannelRegistry lpChannelRegistry;

  public DiscordMessageHandler(ChannelRegistry lpChannelRegistry) {
     this.lpChannelRegistry = lpChannelRegistry;
  }

  public void processSlashCommand(SlashCommandCreateEvent slashCommandCreateEvent) {
    SlashCommandInteraction slashCommandInteraction = slashCommandCreateEvent.getSlashCommandInteraction();
    User user = slashCommandInteraction.getUser();
    if (SpamProtector.checkAuthorOkay(user)) {
      slashCommandInteraction.getChannel()
        .ifPresent(channel -> {
          InteractionOriginalResponseUpdater responder = slashCommandInteraction.respondLater().join();
          String command = slashCommandInteraction.getFullCommandName();
          switch (command) {
            case "start":
              Optional<SlashCommandInteractionOption> optionalCountdown = slashCommandInteraction.getOptionByName("countdown");
              DiscordCountdownHandler.getCountdownSeconds(optionalCountdown)
                .ifPresentOrElse(countdown -> lpChannelRegistry
                  .getExistingLPInstance(channel, responder)
                  .ifPresent(lp -> lp.start(responder, countdown)),
                () -> responder.addEmbed(DiscordUtils.createErrorEmbed("Custom countdown must be between 0-30 seconds")).update());
              break;
            case "stop":
              lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.stop(responder));
              break;
            case "skip":
              lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.skip(responder));
              break;
            case "pause":
              lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.pause(responder));
              break;
            case "nowplaying":
              lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.nowPlaying(responder));
              break;
            case "set":
              slashCommandInteraction.getOptionByName("url")
                .map(SlashCommandInteractionOption::getStringValue)
                .map(Optional::get)
                .ifPresent(url -> lpChannelRegistry.register(channel, responder, url));
              break;
            case "link":
              lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.link(responder));
              break;
            case "totw":
              slashCommandInteraction.getArgumentAttachmentValueByName("attachment")
                .ifPresent(attachment -> lpChannelRegistry.registerTotw(channel, responder, attachment));
              break;
            case "help":
              sendHelpEmbed(responder);
              break;
            default:
              sendBasicUsageEmbed(responder);
              break;
          }
        });
    }
  }

  private void sendHelpEmbed(InteractionOriginalResponseUpdater responder) {
    EmbedBuilder helpEmbed = new EmbedBuilder();
    helpEmbed.setTitle("Listening Party Bot - Help");
    for (DiscordSlashCommands.LPBotCommand command : DiscordSlashCommands.getCommands()) {
      helpEmbed.addField(String.format("`/%s`", command.getCommandWithSubCommand()), "> " + command.getFullDescription());
    }
    responder.addEmbed(helpEmbed).update();
  }

  private void sendBasicUsageEmbed(InteractionOriginalResponseUpdater responder) {
    EmbedBuilder basicUsageEmbed = new EmbedBuilder();
    basicUsageEmbed.setDescription("`/<command>`\n\nSee `/sendHelpEmbed` for more information");
    responder.addEmbed(basicUsageEmbed).update();
  }
}
