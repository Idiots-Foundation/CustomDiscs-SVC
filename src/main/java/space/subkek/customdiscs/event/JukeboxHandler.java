package space.subkek.customdiscs.event;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import space.subkek.customdiscs.LavaPlayerManagerImpl;

public class JukeboxHandler implements Listener {
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
