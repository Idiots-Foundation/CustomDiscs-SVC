package space.subkek.customdiscs.event;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.Keys;
import space.subkek.customdiscs.LavaPlayerManager;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.PlayUtil;

public class JukeboxHandler implements Listener {
  private static ItemStack getItemStack(PlayerInteractEvent event, Player player) {
    ItemStack itemInvolvedInEvent;
    if (event.getMaterial().equals(Material.AIR)) {

      if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
        itemInvolvedInEvent = player.getInventory().getItemInMainHand();
      } else if (!player.getInventory().getItemInOffHand().getType().equals(Material.AIR)) {
        itemInvolvedInEvent = player.getInventory().getItemInOffHand();
      } else {
        itemInvolvedInEvent = new ItemStack(Material.AIR);
      }

    } else {
      itemInvolvedInEvent = new ItemStack(event.getMaterial());
    }
    return itemInvolvedInEvent;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInsert(PlayerInteractEvent event) {
    Block block = event.getClickedBlock();

    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
    if (event.getPlayer().isSneaking()) return;
    if (event.getClickedBlock() == null) return;
    if (event.getItem() == null) return;
    if (!event.getItem().hasItemMeta()) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (LegacyUtil.isJukeboxContainsDisc(block)) return;

    // TODO remove in future versions
    ItemMeta meta = event.getItem().getItemMeta();
    PersistentDataContainer data = meta.getPersistentDataContainer();

    if (data.has(Keys.YOUTUBE_DISC.key(), Keys.YOUTUBE_DISC.dataType())) {
      String url = data.get(Keys.YOUTUBE_DISC.key(), Keys.YOUTUBE_DISC.dataType());
      for (NamespacedKey key : data.getKeys()) {
        data.remove(key);
      }
      data.set(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType(), url);
      event.getItem().setItemMeta(meta);
    } else if (data.has(Keys.SOUNDCLOUD_DISC.key(), Keys.SOUNDCLOUD_DISC.dataType())) {
      String url = data.get(Keys.SOUNDCLOUD_DISC.key(), Keys.SOUNDCLOUD_DISC.dataType());
      for (NamespacedKey key : data.getKeys()) {
        data.remove(key);
      }
      data.set(Keys.REMOTE_DISC.key(), Keys.REMOTE_DISC.dataType(), url);
      event.getItem().setItemMeta(meta);
    }
    // --------------------------------

    boolean isLocalDisc = LegacyUtil.isCustomDisc(event.getItem());
    boolean isRemoteDisc = LegacyUtil.isCustomStreamingDisc(event.getItem());

    if (!isLocalDisc && !isRemoteDisc) return;

    CustomDiscs.debug("Jukebox insert by Player event");

    if (isLocalDisc)
      PlayUtil.playStandard(block, event.getItem());

    if (isRemoteDisc) {
      PlayUtil.playLava(block, event.getItem());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEject(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!LegacyUtil.isJukeboxContainsDisc(block)) return;
    ItemStack itemInvolvedInEvent = getItemStack(event, player);
    if (player.isSneaking() && !itemInvolvedInEvent.getType().equals(Material.AIR)) return;
    Jukebox jukebox = (Jukebox) block.getState();
    if (!LegacyUtil.isCustomDisc(jukebox.getRecord()) &&
        !LegacyUtil.isCustomStreamingDisc(jukebox.getRecord())) return;

    CustomDiscs.debug("Jukebox eject by Player event");

    LavaPlayerManager.getInstance().stopPlaying(block);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJukeboxBreak(BlockBreakEvent event) {

    Block block = event.getBlock();

    if (block.getType() != Material.JUKEBOX) return;

    LavaPlayerManager.getInstance().stopPlaying(block);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJukeboxExplode(EntityExplodeEvent event) {
    for (Block explodedBlock : event.blockList()) {
      if (explodedBlock.getType() == Material.JUKEBOX) {
        LavaPlayerManager.getInstance().stopPlaying(explodedBlock);
      }
    }
  }
}
