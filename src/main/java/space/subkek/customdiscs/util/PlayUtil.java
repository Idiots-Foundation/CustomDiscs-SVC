package space.subkek.customdiscs.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.DiscEntry;

public final class PlayUtil {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void play(final Block block, final DiscEntry disc) {
    play(block, disc, null);
  }

  public static void play(final Block block, final DiscEntry disc, @Nullable final Player player) {
    plugin.getFoliaLib().getScheduler().runAtLocationLater(block.getLocation(), task -> {
      if (block.getState() instanceof final Jukebox jukebox) {
        jukebox.stopPlaying();
      }
    }, 1);

    LavaPlayerManagerImpl.getInstance().playDisc(
      block,
      disc.getIdentifier(),
      plugin.getLanguage().component("now-playing", disc.getName()),
      disc.getName(),
      activationSide(block, player)
    );
  }

  private static BlockFace activationSide(final Block block, @Nullable final Player player) {
    if (player == null || !block.getWorld().equals(player.getWorld())) return BlockFace.SOUTH;

    final var playerLocation = player.getLocation();
    final var deltaX = playerLocation.getX() - (block.getX() + 0.5d);
    final var deltaZ = playerLocation.getZ() - (block.getZ() + 0.5d);
    if (Math.abs(deltaX) > Math.abs(deltaZ)) return deltaX >= 0d ? BlockFace.EAST : BlockFace.WEST;
    return deltaZ >= 0d ? BlockFace.SOUTH : BlockFace.NORTH;
  }
}
