package io.github.subkek.customdiscs.command.subcommand;

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

import java.util.List;

public class CreateYtSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateYtSubCommand() {
    super("createyt");

    this.withFullDescription(getDescription());
    this.withUsage(getUsage());

    this.withArguments(new TextArgument("url"));
    this.withArguments(new TextArgument("song_name"));

    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.createyt.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.createyt.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.createyt");
  }

  @Override
  public void executePlayer(Player player, CommandArguments arguments) {
    if (!CustomDiscs.lavaLibExist) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-youtube-support"));
      return;
    }

    if (!hasPermission(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.error.not-holding-disc"));
      return;
    }

    String customName = getArgumentValue(arguments, "song_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.disc-name-empty"));
      return;
    }

    ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

    ItemMeta meta = LegacyUtil.getItemMeta(disc);

    meta.displayName(plugin.getLanguage().component("disc-name.youtube")
        .decoration(TextDecoration.ITALIC, false));

    final Component customLoreSong = Component.text(customName)
        .decoration(TextDecoration.ITALIC, false)
        .color(NamedTextColor.GRAY);

    meta.addItemFlags(ItemFlag.values());
    meta.lore(List.of(customLoreSong));

    if (plugin.getCDConfig().isUseCustomModelDataYoutube())
      meta.setCustomModelData(plugin.getCDConfig().getCustomModelDataYoutube());

    String youtubeUrl = getArgumentValue(arguments, "url", String.class);

    PersistentDataContainer data = meta.getPersistentDataContainer();
    for (NamespacedKey key : data.getKeys()) {
      data.remove(key);
    }
    data.set(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType(), youtubeUrl);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.link", youtubeUrl));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.create.messages.name", customName));
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
  }
}
