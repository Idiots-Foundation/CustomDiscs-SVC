package space.subkek.customdiscs.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import space.subkek.customdiscs.command.subcommand.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public final class CustomDiscsCommand extends AbstractCommand {
  public CustomDiscsCommand() {
    super("customdiscs");
    this.addSubCommand(new HelpSubCommand(this));
    this.addSubCommand(new ReloadSubCommand());
    this.addSubCommand(new DownloadSubCommand());
    this.addSubCommand(new CreateSubCommand());
    this.addSubCommand(new DistanceSubCommand());
  }

  public void register(final ReloadableRegistrarEvent<Commands> event) {
    final var builder = Commands.literal(this.getName());
    this.build(builder);
    this.buildSubCommands(builder);

    event.registrar().register(builder.build(), "Main CustomDiscs command", List.of("cd"));
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs");
  }

  @Override
  public void build(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    builder.executes(this::execute);
  }

  public int execute(final CommandContext<CommandSourceStack> ctx) {
    return this.findHelpCommand().execute(ctx);
  }

  @NotNull
  private AbstractCommand findHelpCommand() {
    for (final var currentSub : this.getSubCommands()) {
      if (currentSub.getName().equals("help")) {
        return currentSub;
      }
    }
    throw new IllegalStateException("Command help doesn't exist");
  }
}
