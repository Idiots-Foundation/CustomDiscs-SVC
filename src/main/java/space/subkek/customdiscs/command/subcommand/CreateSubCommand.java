package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractCommand;

public final class CreateSubCommand extends AbstractCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateSubCommand() {
    super("create");
    this.addSubCommand(new LocalCreateSubCommand());
    this.addSubCommand(new RemoteCreateSubCommand());
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.create.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.create.syntax");
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.create");
  }

  @Override
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.requires(source -> this.hasPermission(source.getSender()));
  }
}
