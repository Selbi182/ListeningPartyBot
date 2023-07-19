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
import spotify.lpbot.party.registry.ChannelRegistry;

@Component
public class DiscordMessageHandler {
  private static final long DEFAULT_COUNTDOWN_SECONDS = 5L;
  private static final long MAX_COUNTDOWNS_SECONDS = 120L;
  private static final long DEFAULT_SKIP_AMOUNT = 1L;

  private final ChannelRegistry lpChannelRegistry;

  DiscordMessageHandler(ChannelRegistry lpChannelRegistry) {
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
          try {
            switch (command) {
              case "set":
                slashCommandInteraction.getOptionByName("url")
                  .map(SlashCommandInteractionOption::getStringValue)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .ifPresent(url -> lpChannelRegistry.register(channel, responder, url, true));
                break;
              case "start":
                Optional<SlashCommandInteractionOption> optionalCountdown = slashCommandInteraction.getOptionByName("countdown");
                getCountdownSeconds(optionalCountdown)
                  .ifPresentOrElse(countdown -> lpChannelRegistry
                      .getExistingLPInstance(channel, responder)
                      .ifPresent(lp -> lp.start(responder, countdown)),
                    () -> DiscordUtils.updateWithErrorEmbed(responder, "Custom countdown must be between 0-" + MAX_COUNTDOWNS_SECONDS + " seconds"));
                break;
              case "quickstart":
                slashCommandInteraction.getOptionByName("url")
                  .map(SlashCommandInteractionOption::getStringValue)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .map(url -> lpChannelRegistry.register(channel, responder, url, false))
                  .ifPresent(lp -> lp.start(responder, 0));
                break;
              case "stop":
                lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.stop(responder));
                break;
              case "skip":
                Optional<SlashCommandInteractionOption> optionalSkipAmount = slashCommandInteraction.getOptionByName("amount");
                getSkipAmount(optionalSkipAmount)
                  .ifPresentOrElse(skipAmount -> lpChannelRegistry
                      .getExistingLPInstance(channel, responder)
                      .ifPresent(lp -> lp.next(responder, skipAmount)),
                    () -> DiscordUtils.updateWithErrorEmbed(responder, "Invalid amount"));
                break;
              case "previous":
                Optional<SlashCommandInteractionOption> optionalGoBackAmount = slashCommandInteraction.getOptionByName("amount");
                getSkipAmount(optionalGoBackAmount)
                  .ifPresentOrElse(goBackAmount -> lpChannelRegistry
                      .getExistingLPInstance(channel, responder)
                      .ifPresent(lp -> lp.previous(responder, goBackAmount)),
                    () -> DiscordUtils.updateWithErrorEmbed(responder, "Invalid amount"));
                break;
              case "pause":
                lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.pause(responder));
                break;
              case "restart":
                lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.restart(responder));
                break;
              case "np":
                lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.nowPlaying(responder));
                break;
              case "link":
                lpChannelRegistry.getExistingLPInstance(channel, responder).ifPresent(lp -> lp.link(responder));
                break;
              case "custom":
                slashCommandInteraction.getArgumentAttachmentValueByName("attachment")
                  .ifPresent(attachment -> {
                    boolean guessingGame = slashCommandInteraction.getArgumentBooleanValueByName("guessing-game").orElse(false);
                    boolean shuffle = slashCommandInteraction.getArgumentBooleanValueByName("shuffle").orElse(false);
                    lpChannelRegistry.registerCustomLp(channel, responder, attachment, guessingGame, shuffle);
                  });
                break;
              case "help":
                sendTutorialEmbed(responder);
                break;
              case "commands":
                sendCommandsEmbed(responder);
                break;
              default:
                sendBasicUsageEmbed(responder);
                break;
            }
          } catch (Exception e) {
            e.printStackTrace();
            DiscordUtils.updateWithErrorEmbed(responder, "An internal server error occurred");
          }
        });
    }
  }

  private void sendTutorialEmbed(InteractionOriginalResponseUpdater responder) {
    EmbedBuilder tutorialEmbed = new EmbedBuilder();
    tutorialEmbed.setTitle("Listening Party Bot \u2013 Basic Usage");

    tutorialEmbed.setDescription("A listening party can be started in just two simple steps:\n\n"
      + "> **1.** Type " + DiscordUtils.findClickableCommand("set") + " and pass a URL to a Spotify album or playlist. "
      + "A message with the target link will pop up so that everyone can get ready.\n"
      + "> \n"
      + "> **2.** Type " + DiscordUtils.findClickableCommand("start") + " to start the actual listening party. After a short countdown (by default, 5 seconds) the party starts!"
      + "\n\n"
      + "For a list of all commands, type:\n" + DiscordUtils.findClickableCommand("commands") + "\n\n"
      + "For more information, check the homepage:\n" + DiscordUtils.LPBOT_URL_HTTPS);

    DiscordUtils.respondWithEmbed(responder, tutorialEmbed);
  }

  private void sendCommandsEmbed(InteractionOriginalResponseUpdater responder) {
    EmbedBuilder commandsEmbed = new EmbedBuilder();
    commandsEmbed.setTitle("Listening Party Bot \u2013 Commands");
    commandsEmbed.addField("General info and basic tutorial:", "> " + DiscordUtils.LPBOT_URL_HTTPS, false);

    for (DiscordSlashCommands.LPBotCommand command : DiscordSlashCommands.getLpBotCommands()) {
      String commandFormatted = DiscordUtils.asClickableCommand(command.getCommand(), command.getId());
      commandsEmbed.addField(commandFormatted, "> " + command.getFullDescription());
    }
    DiscordUtils.respondWithEmbed(responder, commandsEmbed);
  }

  private void sendBasicUsageEmbed(InteractionOriginalResponseUpdater responder) {
    EmbedBuilder basicUsageEmbed = new EmbedBuilder();
    basicUsageEmbed.setDescription("`/<command>`\n\nSee " + DiscordUtils.findClickableCommand("help") + " for more information");
    DiscordUtils.respondWithEmbed(responder, basicUsageEmbed);
  }

  public static Optional<Integer> getCountdownSeconds(Optional<SlashCommandInteractionOption> customCountdownSeconds) {
    long countdown = Math.toIntExact(customCountdownSeconds
      .map(SlashCommandInteractionOption::getLongValue)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(l -> Math.min(l, Integer.MAX_VALUE))
      .orElse(DEFAULT_COUNTDOWN_SECONDS));

    if (countdown >= 0 && countdown <= MAX_COUNTDOWNS_SECONDS) {
      return Optional.of((int) countdown);
    } else {
      return Optional.empty();
    }
  }

  private Optional<Integer> getSkipAmount(Optional<SlashCommandInteractionOption> optionalSkipAmount) {
    long skipAmount = Math.toIntExact(optionalSkipAmount
      .map(SlashCommandInteractionOption::getLongValue)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(l -> Math.min(l, Integer.MAX_VALUE))
      .orElse(DEFAULT_SKIP_AMOUNT));

    if (skipAmount >= 1) {
      return Optional.of((int) skipAmount);
    } else {
      return Optional.empty();
    }
  }
}
