package space.subkek.customdiscs.listener;

import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.api.event.CustomDiscInsertEvent;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.PlayUtil;

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

    final var ejectEvent = new CustomDiscEjectEvent(block, null, LegacyUtil.getDiscEntry(event.getItem()));
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(ejectEvent);
    event.setCancelled(ejectEvent.isCancelled());
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxInsertByRedstone(final InventoryMoveItemEvent event) {
    if (!CustomDiscs.getPlugin().getCDConfig().isAllowHoppers()) return;
    if (!(event.getDestination().getHolder() instanceof final Jukebox jukebox)) return;
    if (event.getSource().getHolder() instanceof Player) return;
    if (!LegacyUtil.isCustomDisc(event.getItem())) return;

    final var discEntry = LegacyUtil.getDiscEntry(event.getItem());
    final var block = jukebox.getBlock();

    final var insertEvent = new CustomDiscInsertEvent(block, null, LegacyUtil.getDiscEntry(event.getItem()));
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(insertEvent);
    if (!event.isCancelled())
      PlayUtil.play(block, discEntry);
  }
}
