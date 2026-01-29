package space.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;

public class ReloadSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public ReloadSubCommand() {
    super("reload");

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.executes(this::execute);
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
  public void execute(CommandSender sender, CommandArguments arguments) {
    if (!hasPermission(sender)) {
      CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    plugin.getCDConfig().load();
    plugin.getLanguage().load();
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.reload.messages.successfully"));
  }
}
