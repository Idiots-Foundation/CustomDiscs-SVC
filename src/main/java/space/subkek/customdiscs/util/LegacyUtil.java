package space.subkek.customdiscs.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import space.subkek.customdiscs.Keys;

public class LegacyUtil {
  public static boolean isJukeboxContainsDisc(@NotNull Block block) {
    Jukebox jukebox = (Jukebox) block.getLocation().getBlock().getState();
    return jukebox.getRecord().getType() != Material.AIR;
  }

  public static boolean isLocalDisc(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.LOCAL_DISC.key(), Keys.LOCAL_DISC.dataType());
  }

  public static boolean isRemoteDisc(@NotNull ItemStack item) {
    return getItemMeta(item).getPersistentDataContainer()
        .has(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType());
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
}
