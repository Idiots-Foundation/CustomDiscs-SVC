package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PhysicsManager;
import io.github.subkek.customdiscs.PlayerManager;
import io.github.subkek.customdiscs.util.LegacyUtil;
import io.github.subkek.customdiscs.util.PlayUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

public class HopperHandler implements Listener {
  private static HopperHandler instance;

  public synchronized static HopperHandler getInstance() {
    if (instance == null) {
      instance = new HopperHandler();
    }
    return instance;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJukeboxInsertFromHopper(InventoryMoveItemEvent event) {
    if (event.getDestination().getLocation() == null) return;
    Block block = event.getDestination().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (LegacyUtil.isJukeboxContainsDisc(block)) return;

    boolean isCustomDisc = LegacyUtil.isCustomDisc(event.getItem());
    boolean isYouTubeCustomDisc = LegacyUtil.isCustomStreamingDisc(event.getItem());

    if (!isCustomDisc && !isYouTubeCustomDisc) return;

    CustomDiscs.debug("Jukebox insert by hopper/dropper event");

    if (isCustomDisc)
      PlayUtil.playStandard(block, event.getItem());

    if (isYouTubeCustomDisc)
      PlayUtil.playLava(block, event.getItem());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJukeboxEjectToHopper(InventoryMoveItemEvent event) {
    if (event.getSource().getLocation() == null) return;
    Block block = event.getSource().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!event.getItem().hasItemMeta()) return;
    if (!LegacyUtil.isCustomDisc(event.getItem()) && !LegacyUtil.isCustomStreamingDisc(event.getItem())) return;

    event.setCancelled(PlayerManager.getInstance().isPlaying(block) ||
        LavaPlayerManager.getInstance().isPlaying(block));

    if (!event.isCancelled()) CustomDiscs.debug("Jukebox eject by hopper event");
  }

  @EventHandler
  public void onPhysics(BlockPhysicsEvent event) {
    if (!event.getSourceBlock().getType().equals(Material.JUKEBOX)) return;
    Block block = event.getSourceBlock();

    if ((CustomDiscs.lavaLibExist && !LavaPlayerManager.getInstance().isPlaying(block))
        && !PlayerManager.getInstance().isPlaying(block)) {
      return;
    }

    PhysicsManager.NeedUpdate needUpdate = PhysicsManager.getInstance().isNeedUpdate(block);

    boolean reallyNeed = false;
    PhysicsManager.PhysicsJukebox physicsJukebox = null;
    long time = block.getWorld().getTime();
    if (!needUpdate.returnForced()) {
      physicsJukebox = PhysicsManager.getInstance().get(block);
      reallyNeed = physicsJukebox.getLastUpdateTick() == time;
    }

    if (needUpdate.value() || reallyNeed) {
      assert physicsJukebox != null;
      physicsJukebox.setNeedUpdate(false);
      physicsJukebox.setLastUpdateTick(time);
      CustomDiscs.debug("Updating physics on {0} by jukebox", event.getBlock().getType());
      return;
    }

    event.setCancelled(true);
  }
}
