package space.subkek.customdiscs.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import space.subkek.customdiscs.util.Formatter;
import space.subkek.customdiscs.util.RemoteServices;

import java.util.List;

public abstract class AbstractSubCommand extends CommandAPICommand {
  public AbstractSubCommand(String commandName) {
    super(commandName);
  }

  protected <T> T getArgumentValue(CommandArguments arguments, String nodeName, Class<T> argumentType) {
    T value;
    if ((value = arguments.getByClass(nodeName, argumentType)) == null)
      throw new IllegalArgumentException(Formatter.format(
        "Couldn't find argument {0} with name", nodeName
      ));
    return value;
  }

  protected ArgumentSuggestions<CommandSender> quotedArgument(@Nullable List<String> suggestions) {
    return ArgumentSuggestions.stringCollection(info -> {
      String arg = info.currentArg().trim();

      if (arg.isEmpty()) return List.of("\"");

      if (suggestions != null && arg.equals("\""))
        return suggestions.stream().map(s -> "\"" + s + "\"").toList();

      if (arg.startsWith("\"") && !arg.endsWith("\""))
        return List.of(arg + "\"");

      return List.of();
    });
  }

  public void execute(CommandSender sender, CommandArguments arguments) {
  }

  public void executePlayer(Player player, CommandArguments arguments) {
  }

  public abstract String getDescription();

  public abstract String getSyntax();

  public boolean hasPermission(CommandSender sender) {
    return false;
  }

  public boolean hasPermission(CommandSender sender, RemoteServices service) {
    return hasPermission(sender);
  }
}
