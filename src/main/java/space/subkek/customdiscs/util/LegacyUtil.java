package space.subkek.customdiscs.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.Keys;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.DiscEntry;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class LegacyUtil {
  public static boolean isJukeboxContainsDisc(@NotNull Block block) {
    Jukebox jukebox = (Jukebox) block.getLocation().getBlock().getState();
    return jukebox.getRecord().getType() != Material.AIR;
  }

  private static boolean isLocalDisc(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.LOCAL_DISC.key(), Keys.LOCAL_DISC.dataType());
  }

  private static boolean isRemoteDisc(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType());
  }

  public static boolean isCustomDisc(@NotNull ItemStack item) {
    {
      ItemMeta meta = getItemMeta(item);
      if (migratePDC(meta.getPersistentDataContainer()))
        item.setItemMeta(meta);
    }
    return isLocalDisc(item) || isRemoteDisc(item);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isMusicDiscInHand(Player player) {
    return player.getInventory().getItemInMainHand().getType().toString().contains("MUSIC_DISC");
  }

  public static ItemMeta getItemMeta(ItemStack itemStack) {
    ItemMeta meta;

    if ((meta = itemStack.getItemMeta()) == null)
      throw new IllegalStateException("Why item meta is null!?");

    return meta;
  }

  public static UUID getBlockUUID(Block block) {
    return UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
  }

  @SuppressWarnings("unchecked")
  private static boolean migratePDC(PersistentDataContainer data) {
    String legacyLocalValue = data.get(Keys.LEGACY_LOCAL_DISC.key(), Keys.LEGACY_LOCAL_DISC.dataType());
    if (legacyLocalValue != null) {
      data.remove(Keys.LEGACY_LOCAL_DISC.key());
      data.set(Keys.LOCAL_DISC.key(), Keys.LOCAL_DISC.dataType(), legacyLocalValue);
      return true;
    }

    Keys.Key<String>[] legacyRemoteKeys = new Keys.Key[] {
        Keys.LEGACY_REMOTE_DISC,
        Keys.LEGACY_YOUTUBE_DISC,
        Keys.LEGACY_SOUNDCLOUD_DISC
    };

    for (Keys.Key<String> key : legacyRemoteKeys) {
      String legacyRemoteValue = data.get(key.key(), key.dataType());
      if (legacyRemoteValue != null) {
        data.remove(key.key());
        data.set(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType(), legacyRemoteValue);
        return true;
      }
    }

    return false;
  }

  public static DiscEntry getDiscEntry(ItemStack disc) {
    ItemMeta meta = getItemMeta(disc);
    PersistentDataContainer data = meta.getPersistentDataContainer();

    String local = data.get(Keys.LOCAL_DISC.key(), Keys.LOCAL_DISC.dataType());
    if (local != null) {
      File file = new File(CustomDiscs.getPlugin().getMusicData(), local);
      return new DiscEntry(disc, getSongName(meta), file.getPath(), true);
    }

    String remote = data.get(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType());
    if (remote != null) {
      return new DiscEntry(disc, getSongName(meta), remote, false);
    }

    throw new IllegalArgumentException();
  }

  private static Component getSongName(ItemMeta meta) {
    List<Component> lore = meta.lore();
    if (lore == null || lore.isEmpty())
      return Component.text("Unknown").color(NamedTextColor.GRAY);

    return lore.getFirst();
  }
}
