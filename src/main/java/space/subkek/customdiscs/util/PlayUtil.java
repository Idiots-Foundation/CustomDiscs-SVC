package space.subkek.customdiscs.util;

import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.ParticleManager;
import space.subkek.customdiscs.api.DiscEntry;

public class PlayUtil {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void play(Block block, DiscEntry disc) {
    ParticleManager.start(block);

    plugin.getFoliaLib().getScheduler().runAtLocationLater(block.getLocation(), task -> {
      if (block.getState() instanceof Jukebox jukebox) {
        jukebox.stopPlaying();
      }
    }, 1);

    LavaPlayerManagerImpl.getInstance().play(block, disc.getIdentifier(), plugin.getLanguage().component("now-playing", disc.getName()));
  }
}
