package space.subkek.customdiscs.util;

import org.bukkit.block.Block;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.DiscEntry;

public class PlayUtil {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void play(Block block, DiscEntry disc) {
    LavaPlayerManagerImpl.getInstance().play(block, disc.getIdentifier(), plugin.getLanguage().component("now-playing", disc.getName()));
  }
}
