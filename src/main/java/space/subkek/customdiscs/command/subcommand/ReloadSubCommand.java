package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractCommand;

public final class ReloadSubCommand extends AbstractCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public ReloadSubCommand() {
    super("reload");
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.reload.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.reload.syntax");
  }

  @Override
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.requires(source -> this.hasPermission(source.getSender()));
    builder.executes(this::execute);
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.reload");
  }

  @Override
  public int execute(final CommandContext<CommandSourceStack> context) {
    this.plugin.getCDConfig().load();
    this.plugin.getLanguage().load();
    CustomDiscs.sendMessage(context.getSource().getSender(), this.plugin.getLanguage().PComponent("command.reload.messages.successfully"));

    return SINGLE_SUCCESS;
  }
}
