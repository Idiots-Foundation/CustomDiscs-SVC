package space.subkek.customdiscs.command.subcommand;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.command.AbstractSubCommand;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

public class DownloadSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public DownloadSubCommand() {
    super("download");
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> assemble(final LiteralArgumentBuilder<CommandSourceStack> builder) {
    return builder.then(Commands.argument("url", StringArgumentType.string())

      .then(Commands.argument("filename", StringArgumentType.string())
        .executes(this::execute)));
  }

  @Override
  public String getDescription() {
    return this.plugin.getLanguage().string("command.download.description");
  }

  @Override
  public String getSyntax() {
    return this.plugin.getLanguage().string("command.download.syntax");
  }

  @Override
  public boolean hasPermission(final CommandSender sender) {
    return sender.hasPermission("customdiscs.download");
  }

  @Override
  public int execute(final CommandContext<CommandSourceStack> context) {
    final var sender = context.getSource().getSender();

    this.plugin.getFoliaLib().getScheduler().runAsync(task -> {
      try {
        final var fileURL = URI.create(this.getArgumentValue(context, "url", String.class)).toURL();
        final var protocol = fileURL.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
          CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("error.command.invalid-url"));
          return;
        }

        final var filename = this.getArgumentValue(context, "filename", String.class);

        final var base = this.plugin.getDataFolder().toPath().resolve("musicdata").normalize();
        final var resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base)) {
          CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("error.command.invalid-filename"));
          return;
        }

        if (!this.getFileExtension(filename).equals("wav") && !this.getFileExtension(filename).equals("mp3") &&
          !this.getFileExtension(filename).equals("flac")) {
          CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("error.command.unknown-extension"));
          return;
        }

        CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("command.download.messages.downloading"));
        final var downloadPath = Path.of(this.plugin.getDataFolder().getPath(), "musicdata", filename);
        final var downloadFile = new File(downloadPath.toUri());

        final var connection = fileURL.openConnection();

        if (connection != null) {
          final var size = connection.getContentLengthLong() / 1048576;
          if (size > this.plugin.getCDConfig().getMaxDownloadSize()) {
            CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("command.download.messages.error.file-too-large",
              String.valueOf(this.plugin.getCDConfig().getMaxDownloadSize())));
            return;
          }
        }

        FileUtils.copyURLToFile(fileURL, downloadFile);

        CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("command.download.messages.successfully"));
        CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("command.download.messages.create-tooltip",
          this.plugin.getLanguage().string("command.create.syntax")));
      } catch (final Throwable e) {
        CustomDiscs.error("Error while download music: ", e);
        CustomDiscs.sendMessage(sender, this.plugin.getLanguage().PComponent("command.download.messages.error.while-download"));
      }
    });

    return Command.SINGLE_SUCCESS;
  }

  private String getFileExtension(final String s) {
    final var index = s.lastIndexOf(".");
    if (index > 0) {
      return s.substring(index + 1);
    } else {
      return "";
    }
  }
}
