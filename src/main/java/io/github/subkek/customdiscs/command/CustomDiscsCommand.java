package io.github.subkek.customdiscs.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.command.subcommand.*;
import io.github.subkek.customdiscs.util.Formatter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CustomDiscsCommand extends CommandAPICommand {
  public CustomDiscsCommand() {
    super("customdiscs");

    this.withAliases("cd");
    this.withFullDescription("Main command of CustomDiscs-SVC plugin.");

    this.withSubcommand(new HelpSubCommand(this));
    this.withSubcommand(new ReloadSubCommand());
    this.withSubcommand(new DownloadSubCommand());
    this.withSubcommand(new CreateSubCommand());
    this.withSubcommand(new CreateYtSubCommand());
    this.withSubcommand(new CreateScSubCommand());
    this.withSubcommand(new DistanceSubCommand());

    this.executes(this::execute);
  }

  public void execute(CommandSender sender, CommandArguments arguments) {
    findSubCommand("help").execute(sender, arguments);
  }

  @NotNull
  private AbstractSubCommand findSubCommand(String commandName) {
    AbstractSubCommand subCommand = null;

    for (CommandAPICommand caSubCommand : getSubcommands()) {
      if (caSubCommand.getName().equals(commandName)) {
        subCommand = (AbstractSubCommand) caSubCommand;
        break;
      }
    }

    if (subCommand == null)
      throw new IllegalArgumentException(Formatter.format(
          "Command with name {0} doesn't exists!", commandName
      ));

    return subCommand;
  }
}
