package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;
import space.subkek.customdiscs.event.PlayerHandler;

public class DistanceSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public DistanceSubCommand() {
    super("distance");
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> assemble(LiteralArgumentBuilder<CommandSourceStack> builder) {
    int maxDistance = plugin.getCDConfig().getDistanceCommandMaxDistance();

    return builder.then(Commands.argument("radius", IntegerArgumentType.integer(0, maxDistance))
      .executes(this::executePlayer));
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.distance.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.distance.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.distance");
  }

  public int executePlayer(CommandContext<CommandSourceStack> context) {
    CommandSender sender = context.getSource().getSender();

    if (!(sender instanceof Player player)) {
      return execute(context);
    }

    int radius = getArgumentValue(context, "radius", Integer.class);

    PlayerHandler.getInstance().getPlayersSelecting().put(player.getUniqueId(), radius);

    CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.distance.messages.click"));

    return Command.SINGLE_SUCCESS;
  }

  @Override
  public int execute(CommandContext<CommandSourceStack> context) {
    CustomDiscs.sendMessage(context.getSource().getSender(), plugin.getLanguage().PComponent("error.command.cant-perform"));
    return 0;
  }
}
