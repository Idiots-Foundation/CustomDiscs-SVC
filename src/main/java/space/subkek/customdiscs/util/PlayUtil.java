package space.subkek.customdiscs.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.Keys;
import space.subkek.customdiscs.LavaPlayerManager;

import java.nio.file.Path;
import java.util.List;

public class PlayUtil {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void playStandard(Block block, ItemStack disc) {
    plugin.discsPlayed++;

    ItemMeta discMeta = LegacyUtil.getItemMeta(disc);
    String soundFileName = discMeta.getPersistentDataContainer().get(Keys.LOCAL_DISC.key(), Keys.LOCAL_DISC.dataType());
    Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

    if (soundFilePath.toFile().exists()) {
      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", getSongName(discMeta));
      LavaPlayerManager.getInstance().play(block, soundFilePath.toString(), customActionBarSongPlaying);
    }
  }

  public static void playLava(Block block, ItemStack disc) {
    plugin.discsPlayed++;

    ItemMeta discMeta = LegacyUtil.getItemMeta(disc);
    String soundLink = discMeta.getPersistentDataContainer().get(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType());
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
