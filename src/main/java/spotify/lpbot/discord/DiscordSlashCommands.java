package spotify.lpbot.discord;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import com.google.common.collect.ImmutableSet;

public class DiscordSlashCommands {
  public final static List<LPBotCommand> commands = List.of(
    LPBotCommand.of("start", "Start or resume the Listening Party", SlashCommandOption.create(SlashCommandOptionType.LONG, "countdown", "the seconds to count down, default: 5", false)),
    LPBotCommand.of("stop", "Cancel a current Listening Party and reset it to the beginning"),
    LPBotCommand.of("skip", "Skip the current song in the Listening Party", SlashCommandOption.create(SlashCommandOptionType.LONG, "amount", "how many songs to skip, default: 1", false)),
    LPBotCommand.of("pause", "Pause the current Listening Party (resume by typing `/start` again)"),
    LPBotCommand.of("nowplaying", "Print info of the current Listening Party for this channel"),
    LPBotCommand.of("link", "Print the set target link"),
    LPBotCommand.of("set", "Set the target link", SlashCommandOption.create(SlashCommandOptionType.STRING, "url", "the URL to the Spotify playlist or album", true)),
    LPBotCommand.of("help", "Print the commands as chat message"),
    LPBotCommand.of("totw", "[Experimental] Host a Track-of-the-Week party", SlashCommandOption.create(SlashCommandOptionType.ATTACHMENT, "attachment", "the TOTW info data", true))
  );

  public static List<LPBotCommand> getCommands() {
    return commands;
  }

  public static Set<SlashCommandBuilder> getSlashCommands () {
    ImmutableSet.Builder<SlashCommandBuilder> builder = ImmutableSet.builder();
    for (LPBotCommand command : commands) {
      builder.add(command.getSubCommand()
        .map(subCommand -> SlashCommand.with(command.getCommand(), command.getDescription(), List.of(subCommand)))
        .orElse(SlashCommand.with(command.getCommand(), command.getDescription())));
    }
    return builder.build();
  }

  static class LPBotCommand {
    private final String command;
    private final String description;
    private SlashCommandOption subCommand;

    LPBotCommand(String command, String description) {
      this.command = command;
      this.description = description;
    }
    LPBotCommand(String command, String description, SlashCommandOption subCommand) {
      this(command, description);
      this.subCommand = subCommand;
    }

    private static LPBotCommand of(String command, String description) {
      return new LPBotCommand(command, description);
    }

    private static LPBotCommand of(String command, String description, SlashCommandOption subCommand) {
      return new LPBotCommand(command, description, subCommand);
    }

    public String getCommand() {
      return command;
    }

    public String getDescription() {
      return description;
    }

    public Optional<SlashCommandOption> getSubCommand() {
      return Optional.ofNullable(subCommand);
    }

    public String getFullDescription() {
      return getSubCommand()
        .map(subCommand -> String.format("%s (`%s`: %s)", getDescription(), subCommand.getName(), subCommand.getDescription()))
        .orElse(getDescription());
    }

    public String getCommandWithSubCommand() {
      return getSubCommand()
        .map(subCommand -> String.format("%s <%s>", getCommand(), subCommand.getName()))
        .orElse(getCommand());
    }
  }
}
