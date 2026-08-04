package space.subkek.customdiscs.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import space.subkek.customdiscs.command.subcommand.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CustomDiscsCommand {
  private final List<AbstractSubCommand> subcommands = new ArrayList<>();

  public CustomDiscsCommand() {
    this.registerSubcommand(new HelpSubCommand(this));
    this.registerSubcommand(new ReloadSubCommand());
    this.registerSubcommand(new DownloadSubCommand());
    this.registerSubcommand(new CreateSubCommand());
    this.registerSubcommand(new DistanceSubCommand());
  }

  private void registerSubcommand(final AbstractSubCommand subcommand) {
    this.subcommands.add(subcommand);
  }

  public LiteralCommandNode<CommandSourceStack> create() {
    final var builder = Commands.literal("customdiscs")
      .executes(this::execute);

    for (final var subcommand : this.subcommands) {
      var subNode = Commands.literal(subcommand.getName())
        .requires(stack -> subcommand.hasPermission(stack.getSender()));

      subNode = subcommand.assemble(subNode);

      builder.then(subNode);
    }

    return builder.build();
  }

  public int execute(final CommandContext<CommandSourceStack> ctx) {
    return this.findHelpCommand().execute(ctx);
  }

  @NotNull
  private AbstractSubCommand findHelpCommand() {
    for (final var currentSub : this.getSubcommands()) {
      if (currentSub.getName().equals("help")) {
        return currentSub;
      }
    }
    throw new IllegalStateException("Command help doesn't exist");
  }
}
