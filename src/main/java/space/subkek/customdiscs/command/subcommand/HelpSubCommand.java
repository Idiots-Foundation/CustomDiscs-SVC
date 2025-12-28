package space.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;
import space.subkek.customdiscs.command.CustomDiscsCommand;

public class HelpSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final CustomDiscsCommand cdCommand;

  public HelpSubCommand(CustomDiscsCommand cdCommand) {
    super("help");

    this.cdCommand = cdCommand;

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.help.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.help.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.help");
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    if (!hasPermission(sender)) {
      CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    CustomDiscs.sendMessage(sender, plugin.getLanguage().component("command.help.messages.header"));
    for (CommandAPICommand caSubCommand : cdCommand.getSubcommands()) {
      AbstractSubCommand subCommand = (AbstractSubCommand) caSubCommand;
      if (subCommand.hasPermission(sender)) {
        CustomDiscs.sendMessage(sender, plugin.getLanguage().component("command.help.messages.format", subCommand.getSyntax(), subCommand.getDescription()));
      }
    }

    CustomDiscs.sendMessage(sender, plugin.getLanguage().component("command.help.messages.footer"));
  }
}
