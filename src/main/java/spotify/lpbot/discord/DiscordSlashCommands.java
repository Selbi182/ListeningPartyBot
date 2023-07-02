package spotify.lpbot.discord;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;

public class DiscordSlashCommands {
  // NTS: ` doesn't work for slash command descriptions
  private final static List<LPBotCommand> DISCORD_SLASH_COMMANDS = List.of(
    LPBotCommand.of("set", "Set the target link", SlashCommandOption.createStringOption("url", "the URL to the Spotify playlist or album", true)),
    LPBotCommand.of("start", "Start or resume the Listening Party", SlashCommandOption.createLongOption("countdown", "the seconds to count down, default: 5", false)),
    LPBotCommand.of("quickstart", "A combination of /set and /start to instantly start a Listening Party without countdown", SlashCommandOption.createStringOption("url", "the URL to the Spotify playlist or album", true)),
    LPBotCommand.of("stop", "Cancel a current Listening Party and reset it to the beginning"),
    LPBotCommand.of("skip", "Skip the current song in the Listening Party", SlashCommandOption.createLongOption("amount", "how many songs to skip, default: 1", false)),
    LPBotCommand.of("previous", "Play the previous song in the Listening Party", SlashCommandOption.createLongOption("amount", "how many songs to go back, default: 1", false)),
    LPBotCommand.of("restart", "Restart the currently playing song"),
    LPBotCommand.of("pause", "Pause the current Listening Party (resume by typing /start again)"),
    LPBotCommand.of("np", "Print info of the current Listening Party for this channel (\"now playing\")"),
    LPBotCommand.of("link", "Print the set target link"),
    LPBotCommand.of("help", "Print a basic tutorial of how the bot works"),
    LPBotCommand.of("commands", "Print all commands as a chat message"),
    LPBotCommand.of("custom", "[Experimental] Host a party custom-defined by the given attachment",
      SlashCommandOption.createAttachmentOption("attachment", "the custom data", true),
      SlashCommandOption.createBooleanOption("shuffle", "shuffles the playlist beforehand", false),
      SlashCommandOption.createBooleanOption("guessing-game", "Enable a 30s guessing game before each track", false))
  );

  public static List<LPBotCommand> getLpBotCommands() {
    return DISCORD_SLASH_COMMANDS;
  }

  public static Set<SlashCommandBuilder> getSlashCommands() {
    Set<SlashCommandBuilder> builder = new HashSet<>();
    for (LPBotCommand command : DISCORD_SLASH_COMMANDS) {
      if (command.getSubCommands().isEmpty()) {
        builder.add(SlashCommand.with(command.getCommand(), command.getDescription()));
      } else {
        List<SlashCommandOption> subCommands = command.getSubCommands();
        builder.add(SlashCommand.with(command.getCommand(), command.getDescription(), subCommands));
      }
    }
    return Set.copyOf(builder);
  }

  @SuppressWarnings("SameParameterValue")
  public static class LPBotCommand {
    private final String command;
    private final String description;
    private final List<SlashCommandOption> subCommands;

    private Long id;

    LPBotCommand(String command, String description, SlashCommandOption... subCommands) {
      this.command = command;
      this.description = description;
      this.subCommands = List.of(subCommands);
    }

    private static LPBotCommand of(String command, String description) {
      return new LPBotCommand(command, description);
    }

    private static LPBotCommand of(String command, String description, SlashCommandOption... subCommands) {
      return new LPBotCommand(command, description, subCommands);
    }

    public String getCommand() {
      return command;
    }

    public String getDescription() {
      return description;
    }

    public List<SlashCommandOption> getSubCommands() {
      return subCommands;
    }

    public String getFullDescription() {
      if (getSubCommands().isEmpty()) {
        return getDescription();
      }
      String subCommandsDescription = getSubCommands().stream()
        .map(subCommand -> String.format("`%s`: %s", subCommand.getName(), subCommand.getDescription()))
        .collect(Collectors.joining(" // "));
      return String.format("%s (%s)", getDescription(), subCommandsDescription);
    }

    public void setId(Long id) {
      this.id = id;
    }

    public Long getId() {
      return id;
    }
  }
}
