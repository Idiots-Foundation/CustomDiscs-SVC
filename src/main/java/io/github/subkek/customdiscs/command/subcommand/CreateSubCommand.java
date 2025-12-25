package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import org.bukkit.command.CommandSender;

public class CreateSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateSubCommand() {
    super("create");

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());
    this.withSubcommand(new LocalCreateSubCommand());
    this.withSubcommand(new RemoteCreateSubCommand());
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.create.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.create.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.create");
  }
}
