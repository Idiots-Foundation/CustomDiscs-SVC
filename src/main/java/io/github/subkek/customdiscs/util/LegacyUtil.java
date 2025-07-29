package io.github.subkek.customdiscs.util;

import io.github.subkek.customdiscs.Keys;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class LegacyUtil {
  public static boolean isJukeboxContainsDisc(@NotNull Block block) {
    Jukebox jukebox = (Jukebox) block.getLocation().getBlock().getState();
    return jukebox.getRecord().getType() != Material.AIR;
  }

  public static boolean isCustomDisc(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType());
  }

  public static boolean isCustomYouTubeDisc(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType());
  }

  public static boolean isCustomHorn(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.CUSTOM_HORN.getKey(), Keys.CUSTOM_HORN.getDataType());
  }

  public static boolean isCustomYouTubeHorn(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.YOUTUBE_HORN.getKey(), Keys.YOUTUBE_HORN.getDataType());
  }

  public static boolean isMusicDiscInHand(Player player) {
    return player.getInventory().getItemInMainHand().getType().toString().contains("MUSIC_DISC");
  }

  public static ItemMeta getItemMeta(ItemStack itemStack) {
    ItemMeta meta;

    if ((meta = itemStack.getItemMeta()) == null)
      throw new IllegalStateException("Why item meta is null!?");

    return meta;
  }
}
