package space.subkek.customdiscs.event;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import space.subkek.customdiscs.LavaPlayerManagerImpl;

public final class JukeboxHandler implements Listener {
  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxBreak(final BlockBreakEvent event) {
    final var block = event.getBlock();
    if (block.getType() == Material.JUKEBOX) {
      LavaPlayerManagerImpl.getInstance().stopPlaying(block);
    }
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onJukeboxExplode(final EntityExplodeEvent event) {
    for (final var explodedBlock : event.blockList()) {
      if (explodedBlock.getType() == Material.JUKEBOX) {
        LavaPlayerManagerImpl.getInstance().stopPlaying(explodedBlock);
      }
    }
  }
}
