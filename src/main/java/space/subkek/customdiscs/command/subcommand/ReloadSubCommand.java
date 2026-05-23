package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;

public class ReloadSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public ReloadSubCommand() {
    super("reload");
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> assemble(LiteralArgumentBuilder<CommandSourceStack> builder) {
    return builder.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.reload.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.reload.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.reload");
  }

  @Override
  public int execute(CommandContext<CommandSourceStack> context) {
    plugin.getCDConfig().load();
    plugin.getLanguage().load();
    CustomDiscs.sendMessage(context.getSource().getSender(), plugin.getLanguage().PComponent("command.reload.messages.successfully"));

    return Command.SINGLE_SUCCESS;
  }
}
