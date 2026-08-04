package space.subkek.customdiscs.listener;

import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.util.LegacyUtil;

public final class HopperListener implements Listener {
  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxEjectToHopper(final InventoryMoveItemEvent event) {
    if (!(event.getSource().getHolder() instanceof final Jukebox jukebox)) return;
    if (!LegacyUtil.isCustomDisc(event.getItem())) return;

    final var block = jukebox.getBlock();
    if (LavaPlayerManagerImpl.getInstance().isPlaying(block)) {
      event.setCancelled(true);
      return;
    }

    final var stopEvent = new CustomDiscEjectEvent(block, null, LegacyUtil.getDiscEntry(event.getItem()));
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(stopEvent);
    event.setCancelled(stopEvent.isCancelled());
  }
}
