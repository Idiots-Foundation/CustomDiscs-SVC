package space.subkek.customdiscs.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractCommand {
  @Getter
  private final String name;
  private final List<AbstractCommand> subCommands = new ArrayList<>();

  protected static final int SINGLE_SUCCESS = Command.SINGLE_SUCCESS;

  public AbstractCommand(final String name) {
    this.name = name;
  }

  public List<AbstractCommand> getSubCommands() {
    return List.copyOf(this.subCommands);
  }

  public void addSubCommand(final AbstractCommand subcommand) {
    this.subCommands.add(subcommand);
  }

  public boolean hasPermission(final CommandSender sender) {
    return false;
  }

  protected <T> T getArgumentValue(final CommandContext<CommandSourceStack> context, final String node, final Class<T> type) {
    return context.getArgument(node, type);
  }

  protected SuggestionProvider<CommandSourceStack> quotedArgument(final List<String> suggestions) {
    return this.quotedArgument(() -> suggestions);
  }

  protected SuggestionProvider<CommandSourceStack> quotedArgument(final Supplier<List<String>> suggestions) {
    return (context, builder) -> {
      final var remaining = builder.getRemaining();

      if (remaining.isEmpty()) {
        builder.suggest("\"");
      } else if (remaining.charAt(0) == '"' && remaining.charAt(remaining.length() - 1) != '"') {
        builder.suggest(remaining + '"');
      }

      final var query = remaining.startsWith("\"") ? remaining.substring(1) : remaining;
      for (final var suggestion : suggestions.get()) {
        if (suggestion.regionMatches(true, 0, query, 0, query.length())) {
          builder.suggest("\"" + suggestion + "\"");
        }
      }

      return builder.buildFuture();
    };
  }

  public String getDescription() {
    return "noop";
  }

  public String getSyntax() {
    return "noop";
  }

  public abstract void build(final LiteralArgumentBuilder<CommandSourceStack> builder);

  /**
   * Adds all registered subcommands to the provided Brigadier command builder.
   * <p>
   * This method should be called once from the root command while building the command tree.
   * Each subcommand is added with its own permission check and nested subcommands.
   *
   * @param builder the root command builder
   */
  public void buildSubCommands(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    if (!this.subCommands.isEmpty()) {
      for (final var command : this.subCommands) {
        final var node = Commands.literal(command.getName());

        node.requires(stack -> command.hasPermission(stack.getSender()));
        command.build(node);
        command.buildSubCommands(node);

        builder.then(node);
      }
    }
  }

  public int execute(final CommandContext<CommandSourceStack> context) {
    return SINGLE_SUCCESS;
  }
}
