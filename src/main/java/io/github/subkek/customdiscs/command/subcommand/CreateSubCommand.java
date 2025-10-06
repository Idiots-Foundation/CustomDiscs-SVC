package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.Keys;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import io.github.subkek.customdiscs.util.LegacyUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CreateSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateSubCommand() {
    super("create");

    this.withFullDescription(getDescription());
    this.withUsage(getUsage());

    this.withArguments(new StringArgument("filename").replaceSuggestions(ArgumentSuggestions.stringCollection((sender) -> {
      File musicDataFolder = new File(this.plugin.getDataFolder(), "musicdata");
      if (!musicDataFolder.isDirectory()) {
        return List.of();
      }

      File[] files = musicDataFolder.listFiles();
      if (files == null) {
        return List.of();
      }

      return Arrays.stream(files).filter(file -> !file.isDirectory()).map(File::getName).toList();
    })));
    this.withArguments(new TextArgument("song_name"));

    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
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

  @Override
  public void executePlayer(Player player, CommandArguments arguments) {
    if (!hasPermission(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.error.not-holding-disc"));
      return;
    }

    String filename = getArgumentValue(arguments, "filename", String.class);
    if (filename.contains("../")) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.invalid-filename"));
      return;
    }

    String customName = getArgumentValue(arguments, "song_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.disc-name-empty"));
      return;
    }

    File getDirectory = new File(CustomDiscs.getPlugin().getDataFolder(), "musicdata");
    File songFile = new File(getDirectory.getPath(), filename);
    if (songFile.exists()) {
      if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
        CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.unknown-extension"));
        return;
      }
    } else {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.file.not-found"));
      return;
    }

    //Sets the lore of the item to the quotes from the command.
    ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

    ItemMeta meta = LegacyUtil.getItemMeta(disc);

    meta.displayName(plugin.getLanguage().component("disc-name.simple")
        .decoration(TextDecoration.ITALIC, false));

    final Component customLoreSong = Component.text(customName)
        .decoration(TextDecoration.ITALIC, false)
        .color(NamedTextColor.GRAY);

    meta.addItemFlags(ItemFlag.values());
    meta.lore(List.of(customLoreSong));

    if (plugin.getCDConfig().isUseCustomModelDataYoutube())
      meta.setCustomModelData(plugin.getCDConfig().getCustomModelDataYoutube());

    PersistentDataContainer data = meta.getPersistentDataContainer();
    for (NamespacedKey key : data.getKeys()) {
      data.remove(key);
    }
    data.set(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType(), filename);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.file", filename));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.name", customName));
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
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
