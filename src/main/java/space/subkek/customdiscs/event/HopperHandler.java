package space.subkek.customdiscs.event;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.DiscEntry;
import space.subkek.customdiscs.api.event.CustomDiscInsertEvent;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.file.CDConfig;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.PlayUtil;

public class HopperHandler implements Listener {
  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxInsertFromHopper(InventoryMoveItemEvent event) {
    if (!CustomDiscs.getPlugin().getCDConfig().isAllowHoppers()) return;
    if (event.getDestination().getLocation() == null) return;
    Block block = event.getDestination().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (LegacyUtil.isJukeboxContainsDisc(block)) return;

    if (!LegacyUtil.isCustomDisc(event.getItem())) return;
    DiscEntry discEntry = LegacyUtil.getDiscEntry(event.getItem());

    CustomDiscInsertEvent playEvent = new CustomDiscInsertEvent(block, null, discEntry);
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(playEvent);
    if (!playEvent.isCancelled())
      PlayUtil.play(block, discEntry);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxEjectToHopper(InventoryMoveItemEvent event) {
    if (event.getSource().getLocation() == null) return;
    Block block = event.getSource().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!event.getItem().hasItemMeta()) return;
    if (!LegacyUtil.isCustomDisc(event.getItem())) return;

    if (!LavaPlayerManagerImpl.getInstance().isPlaying(block)) {
      CustomDiscEjectEvent stopEvent = new CustomDiscEjectEvent(block, null, LegacyUtil.getDiscEntry(event.getItem()));
      CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(stopEvent);
      event.setCancelled(stopEvent.isCancelled());
    } else event.setCancelled(true);
  }
}
