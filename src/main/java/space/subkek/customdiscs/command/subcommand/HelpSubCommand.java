package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractCommand;
import space.subkek.customdiscs.command.CustomDiscsCommand;

import java.util.List;

public final class HelpSubCommand extends AbstractCommand {
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
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.requires(source -> this.hasPermission(source.getSender()));
    builder.executes(this::execute);
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.help");
  }

  public int execute(final CommandContext<CommandSourceStack> context) {
    final var sender = context.getSource().getSender();

    CustomDiscs.sendMessage(sender, this.plugin.getLanguage().component("command.help.messages.header"));
    this.printHelp(sender, this.cdCommand.getSubCommands());
    CustomDiscs.sendMessage(sender, this.plugin.getLanguage().component("command.help.messages.footer"));

    return SINGLE_SUCCESS;
  }

  private void printHelp(final CommandSender sender, final List<AbstractCommand> commands) {
    for (final var subCommand : commands) {

      if (subCommand.hasPermission(sender)) {
        if (!subCommand.getSubCommands().isEmpty()) {
          this.printHelp(sender, subCommand.getSubCommands());
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
