package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;
import space.subkek.customdiscs.command.CustomDiscsCommand;

import java.util.List;

public class HelpSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final CustomDiscsCommand cdCommand;

  public HelpSubCommand(CustomDiscsCommand cdCommand) {
    super("help");

    this.cdCommand = cdCommand;
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

  public int execute(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();

    CustomDiscs.sendMessage(sender, plugin.getLanguage().component("command.help.messages.header"));
    printHelp(sender, cdCommand.getSubcommands());
    CustomDiscs.sendMessage(sender, plugin.getLanguage().component("command.help.messages.footer"));

    return Command.SINGLE_SUCCESS;
  }

  private void printHelp(CommandSender sender, List<AbstractSubCommand> commands) {
    for (AbstractSubCommand subCommand : commands) {

      if (subCommand.hasPermission(sender)) {
        if (!subCommand.getSubcommands().isEmpty()) {
          printHelp(sender, subCommand.getSubcommands());
        } else {
          CustomDiscs.sendMessage(sender, plugin.getLanguage().component(
            "command.help.messages.format",
            subCommand.getSyntax(),
            subCommand.getDescription()
          ));
        }
      }
    }
  }
}
