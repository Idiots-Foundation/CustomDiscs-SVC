package space.subkek.customdiscs;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Particle;
import org.bukkit.block.Block;

public final class ParticleManager {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static WrappedTask start(final Block block) {
    final var world = block.getWorld();
    final var location = block.getLocation().add(0.5, 1.2, 0.5);
    return plugin.getFoliaLib().getScheduler().runAtLocationTimer(
      location,
      () -> world.spawnParticle(Particle.NOTE, location, 1),
      1,
      20
    );
  }
}
