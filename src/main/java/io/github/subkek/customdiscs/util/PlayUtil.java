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
import org.bukkit.persistence.PersistentDataContainer;

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

    PersistentDataContainer container = discMeta.getPersistentDataContainer();

    String soundLink;
    if (container.has(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType())) {
      soundLink = container.get(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType());
    } else {
      soundLink = container.get(Keys.SOUNDCLOUD_DISC.getKey(), Keys.SOUNDCLOUD_DISC.getDataType());
    }

    Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", getSongName(discMeta));

    LavaPlayerManager.getInstance().play(block, soundLink, customActionBarSongPlaying);
  }

  private static Component getSongName(ItemMeta discMeta) {
    List<Component> lore = discMeta.lore();
    if (lore == null || lore.isEmpty())
      return Component.text("Unknown").color(NamedTextColor.GRAY);

    return lore.get(0);
  }
}
