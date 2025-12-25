package io.github.subkek.customdiscs.util;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.Keys;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.Path;
import java.util.List;

public class PlayUtil {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void playStandard(Block block, ItemStack disc) {
    plugin.discsPlayed++;

    ItemMeta discMeta = LegacyUtil.getItemMeta(disc);

    String soundFileName = discMeta.getPersistentDataContainer()
        .get(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType());

    Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

    if (soundFilePath.toFile().exists()) {
      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", getSongName(discMeta));

      PlayerManager.getInstance().play(soundFilePath, block, customActionBarSongPlaying);
    }
  }

  public static void playLava(Block block, ItemStack disc) {
    plugin.discsPlayed++;

    ItemMeta discMeta = LegacyUtil.getItemMeta(disc);

    String soundLink = discMeta.getPersistentDataContainer()
      .get(Keys.REMOTE_DISC.getKey(), Keys.REMOTE_DISC.getDataType());

    Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", getSongName(discMeta));

    LavaPlayerManager.getInstance().play(block, soundLink, customActionBarSongPlaying);
  }

  private static Component getSongName(ItemMeta discMeta) {
    List<Component> lore = discMeta.lore();
    if (lore == null || lore.isEmpty())
      return Component.text("Unknown").color(NamedTextColor.GRAY);

    return lore.getFirst();
  }
}
