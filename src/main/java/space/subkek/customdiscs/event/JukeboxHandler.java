package space.subkek.customdiscs.event;

import org.bukkit.Material;
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
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.DiscEntry;
import space.subkek.customdiscs.api.event.CustomDiscInsertEvent;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.PlayUtil;

public class JukeboxHandler implements Listener {
  @EventHandler(priority = EventPriority.NORMAL)
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

    if (!LegacyUtil.isCustomDisc(event.getItem())) return;

    CustomDiscs.debug("Jukebox insert by Player event");

    DiscEntry discEntry = LegacyUtil.getDiscEntry(event.getItem());

    CustomDiscInsertEvent playEvent = new CustomDiscInsertEvent(block, event.getPlayer(), discEntry);
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(playEvent);
    if (!playEvent.isCancelled())
      PlayUtil.play(block, discEntry);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onEject(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (block == null) return;
    if (block.getType() != Material.JUKEBOX) return;
    if (!LegacyUtil.isJukeboxContainsDisc(block)) return;
    ItemStack item = event.getItem() != null ? event.getItem() : new ItemStack(Material.AIR);
    if (player.isSneaking() && item.getType() != Material.AIR) return;
    Jukebox jukebox = (Jukebox) block.getState();
    if (!LegacyUtil.isCustomDisc(jukebox.getRecord())) return;

    CustomDiscs.debug("Jukebox eject by Player event");

    CustomDiscEjectEvent stopEvent = new CustomDiscEjectEvent(block, event.getPlayer(), LegacyUtil.getDiscEntry(jukebox.getRecord()));
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(stopEvent);

    if (stopEvent.isCancelled()) {
      event.setCancelled(true);
      return;
    }

    LavaPlayerManagerImpl.getInstance().stopPlaying(block);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    if (block.getType() == Material.JUKEBOX) {
      LavaPlayerManagerImpl.getInstance().stopPlaying(block);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxExplode(EntityExplodeEvent event) {
    for (Block explodedBlock : event.blockList()) {
      if (explodedBlock.getType() == Material.JUKEBOX) {
        LavaPlayerManagerImpl.getInstance().stopPlaying(explodedBlock);
      }
    }
  }
}
