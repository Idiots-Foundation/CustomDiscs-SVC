package space.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;
import space.subkek.customdiscs.event.PlayerHandler;

public class DistanceSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public DistanceSubCommand() {
    super("distance");

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.withArguments(new IntegerArgument("radius", 0,
      plugin.getCDConfig().getDistanceCommandMaxDistance()));

    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
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

  @Override
  public void executePlayer(Player player, CommandArguments arguments) {
    if (!hasPermission(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    PlayerHandler.getInstance().getPlayersSelecting().put(player.getUniqueId(),
      getArgumentValue(arguments, "radius", Integer.class));

    CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.distance.messages.click"));
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
  }
}
