package space.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
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

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.withArguments(new TextArgument("url"));
    this.withArguments(new StringArgument("filename"));

    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.download.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.download.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.download");
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    if (!hasPermission(sender)) {
      CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    plugin.getFoliaLib().getScheduler().runAsync(task -> {
      try {
        URL fileURL = URI.create(getArgumentValue(arguments, "url", String.class)).toURL();
        String protocol = fileURL.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
          CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.invalid-url"));
          return;
        }

        String filename = getArgumentValue(arguments, "filename", String.class);

        Path base = plugin.getDataFolder().toPath().resolve("musicdata").normalize();
        Path resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base)) {
          CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.invalid-filename"));
          return;
        }

        if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") &&
          !getFileExtension(filename).equals("flac")) {
          CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.unknown-extension"));
          return;
        }

        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.downloading"));
        Path downloadPath = Path.of(plugin.getDataFolder().getPath(), "musicdata", filename);
        File downloadFile = new File(downloadPath.toUri());

        URLConnection connection = fileURL.openConnection();

        if (connection != null) {
          long size = connection.getContentLengthLong() / 1048576;
          if (size > plugin.getCDConfig().getMaxDownloadSize()) {
            CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.error.file-too-large",
              String.valueOf(plugin.getCDConfig().getMaxDownloadSize())));
            return;
          }
        }

        FileUtils.copyURLToFile(fileURL, downloadFile);

        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.successfully"));
        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.create-tooltip",
          plugin.getLanguage().string("command.create.syntax")));
      } catch (Throwable e) {
        CustomDiscs.error("Error while download music: ", e);
        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.error.while-download"));
      }
    });
  }

  private String getFileExtension(String s) {
    int index = s.lastIndexOf(".");
    if (index > 0) {
      return s.substring(index + 1);
    } else {
      return "";
    }
  }
}
