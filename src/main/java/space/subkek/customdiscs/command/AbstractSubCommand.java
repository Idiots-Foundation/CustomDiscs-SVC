package space.subkek.customdiscs.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import space.subkek.customdiscs.util.RemoteServices;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSubCommand {
  private final String commandName;
  @Getter
  private final List<AbstractSubCommand> subcommands = new ArrayList<>();

  public AbstractSubCommand(String commandName) {
    this.commandName = commandName;
  }

  public String getName() {
    return commandName;
  }

  public void addSubcommand(AbstractSubCommand subcommand) {
    this.subcommands.add(subcommand);
  }

  public LiteralArgumentBuilder<CommandSourceStack> assemble(LiteralArgumentBuilder<CommandSourceStack> builder) {
    if (!subcommands.isEmpty()) {
      for (AbstractSubCommand sub : subcommands) {
        LiteralArgumentBuilder<CommandSourceStack> subNode = io.papermc.paper.command.brigadier.Commands.literal(sub.getName())
          .requires(stack -> sub.hasPermission(stack.getSender()));

        subNode = sub.assemble(subNode);

        builder.then(subNode);
      }
    } else {
      builder.executes(this::execute);
    }

    return builder;
  }

  protected <T> T getArgumentValue(CommandContext<CommandSourceStack> context, String nodeName, Class<T> argumentType) {
    try {
      return context.getArgument(nodeName, argumentType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Couldn't find argument %s".formatted(nodeName), e);
    }
  }

  protected SuggestionProvider<CommandSourceStack> quotedArgument(@Nullable List<String> suggestions) {
    return (context, builder) -> {
      String remaining = builder.getRemaining();

      if (remaining.isEmpty()) {
        builder.suggest("\"");
        return builder.buildFuture();
      }

      if (remaining.endsWith("\"") && remaining.length() > 1) {
        return builder.buildFuture();
      }

      String query = remaining.startsWith("\"") ? remaining.substring(1) : remaining;
      String lowerQuery = query.toLowerCase();

      boolean hasMatches = false;

      if (suggestions != null && !suggestions.isEmpty()) {
        for (String s : suggestions) {
          if (s.toLowerCase().startsWith(lowerQuery)) {
            builder.suggest("\"" + s + "\"");
            hasMatches = true;
          }
        }
      }

      if (!hasMatches && remaining.startsWith("\"")) {
        builder.suggest(remaining + "\"");
      }

      return builder.buildFuture();
    };
  }

  public int execute(CommandContext<CommandSourceStack> context) {
    return 1;
  }

  public int executePlayer(CommandContext<CommandSourceStack> context) {
    return 1;
  }

  public abstract String getDescription();

  public abstract String getSyntax();

  public boolean hasPermission(CommandSender sender) {
    return false;
  }

  public boolean hasPermission(CommandSender sender, RemoteServices service) {
    return hasPermission(sender);
  }
}
