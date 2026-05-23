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
    registerSubcommand(new HelpSubCommand(this));
    registerSubcommand(new ReloadSubCommand());
    registerSubcommand(new DownloadSubCommand());
    registerSubcommand(new CreateSubCommand());
    registerSubcommand(new DistanceSubCommand());
  }

  private void registerSubcommand(AbstractSubCommand subcommand) {
    this.subcommands.add(subcommand);
  }

  public LiteralCommandNode<CommandSourceStack> create() {
    LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("customdiscs")
      .executes(this::execute);

    for (AbstractSubCommand subcommand : subcommands) {
      LiteralArgumentBuilder<CommandSourceStack> subNode = Commands.literal(subcommand.getName())
        .requires(stack -> subcommand.hasPermission(stack.getSender()));

      subNode = subcommand.assemble(subNode);

      builder.then(subNode);
    }

    return builder.build();
  }

  public int execute(CommandContext<CommandSourceStack> ctx) {
    return findHelpCommand().execute(ctx);
  }

  @NotNull
  private AbstractSubCommand findHelpCommand() {
    for (AbstractSubCommand currentSub : getSubcommands()) {
      if (currentSub.getName().equals("help")) {
        return currentSub;
      }
    }
    throw new IllegalStateException("Command help doesn't exist");
  }
}
