package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractCommand;
import space.subkek.customdiscs.listener.PlayerListener;

public final class DistanceSubCommand extends AbstractCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public DistanceSubCommand() {
    super("distance");
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.distance.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.distance.syntax");
  }

  @Override
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    final var maxDistance = this.plugin.getCDConfig().getDistanceCommandMaxDistance();

    builder.requires(source -> this.hasPermission(source.getSender()));
    builder
      .then(Commands.argument("radius", IntegerArgumentType.integer(0, maxDistance))
        .executes(this::executePlayer));
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.distance");
  }

  public int executePlayer(final CommandContext<CommandSourceStack> context) {
    final var sender = context.getSource().getSender();

    if (!(sender instanceof final Player player)) {
      return this.execute(context);
    }

    final int radius = this.getArgumentValue(context, "radius", Integer.class);

    PlayerListener.getInstance().getPlayersSelecting().put(player.getUniqueId(), radius);

    CustomDiscs.sendMessage(player, this.plugin.getLanguage().PComponent("command.distance.messages.click"));

    return SINGLE_SUCCESS;
  }

  @Override
  public int execute(final CommandContext<CommandSourceStack> context) {
    CustomDiscs.sendMessage(context.getSource().getSender(), this.plugin.getLanguage().PComponent("error.command.cant-perform"));
    return 0;
  }
}
