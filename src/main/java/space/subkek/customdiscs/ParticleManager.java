package space.subkek.customdiscs;

import org.bukkit.Particle;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;

public final class ParticleManager {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();
  private static final Set<Block> blocks = new HashSet<>();

  public static void start(final Block block) {
    if (blocks.add(block)) {
      final var world = block.getWorld();
      final var location = block.getLocation().add(0.5, 1.2, 0.5);
      plugin.getFoliaLib().getScheduler().runAtLocationTimer(location, task -> {
        if (!LavaPlayerManagerImpl.getInstance().isPlaying(block)) {
          blocks.remove(block);
          task.cancel();
          return;
        }
        world.spawnParticle(Particle.NOTE, location, 1);
      }, 1, 20);
    }
  }
}
