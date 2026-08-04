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

  public HelpSubCommand(final CustomDiscsCommand cdCommand) {
    super("help");

    this.cdCommand = cdCommand;
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.help.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.help.syntax");
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.help");
  }

  public int execute(final CommandContext<CommandSourceStack> context) {
    final var sender = context.getSource().getSender();

    CustomDiscs.sendMessage(sender, this.plugin.getLanguage().component("command.help.messages.header"));
    this.printHelp(sender, this.cdCommand.getSubcommands());
    CustomDiscs.sendMessage(sender, this.plugin.getLanguage().component("command.help.messages.footer"));

    return Command.SINGLE_SUCCESS;
  }

  private void printHelp(final CommandSender sender, final List<AbstractSubCommand> commands) {
    for (final var subCommand : commands) {

      if (subCommand.hasPermission(sender)) {
        if (!subCommand.getSubcommands().isEmpty()) {
          this.printHelp(sender, subCommand.getSubcommands());
        } else {
          CustomDiscs.sendMessage(sender, this.plugin.getLanguage().component(
            "command.help.messages.format",
            subCommand.getSyntax(),
            subCommand.getDescription()
          ));
        }
      }
    }
  }
}
