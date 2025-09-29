package io.github.subkek.customdiscs;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhysicsManager {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final Map<UUID, PhysicsJukebox> jukeboxMap = new HashMap<>();

  private static PhysicsManager instance;

  public synchronized static PhysicsManager getInstance() {
    if (instance == null) return instance = new PhysicsManager();
    return instance;
  }

  public PhysicsJukebox get(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    PhysicsJukebox physicsJukebox = jukeboxMap.get(uuid);
    if (physicsJukebox == null)
      throw new IllegalStateException("This PhysicsJukebox doesn't exists cannot get");
    return physicsJukebox;
  }

  public record NeedUpdate(boolean returnForced, boolean value) {
  }

  public NeedUpdate isNeedUpdate(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    PhysicsJukebox physicsJukebox = jukeboxMap.get(uuid);
    if (physicsJukebox == null) {
      CustomDiscs.debug("PhysicsManager return needUpdate false because ParticleJukebox is null");
      return new NeedUpdate(true, false);
    }
    return new NeedUpdate(false, physicsJukebox.isNeedUpdate());
  }

  private void discToHopper(Block block) {
    if (!plugin.getCDConfig().isAllowHoppers()) return;
    if (!plugin.isEnabled()) return;
    if (!block.getLocation().getChunk().isLoaded()) return;

    Block possibleHopper = block.getRelative(BlockFace.DOWN);
    if (!possibleHopper.getType().equals(Material.HOPPER)) return;

    possibleHopper.getState().update();

    CustomDiscs.debug("Attempting to send a disk to the hopper using a update.");
  }

  private synchronized void stop(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    if (jukeboxMap.containsKey(uuid)) {
      PhysicsJukebox physicsJukebox = jukeboxMap.remove(uuid);
      physicsJukebox.task.cancel();

      if (!block.getType().equals(Material.JUKEBOX)) return;

      Jukebox jukebox = (Jukebox) block.getState();
      jukebox.update();
      jukebox.stopPlaying();
      discToHopper(block);
    }
  }

  public void start(Jukebox jukebox) {
    UUID uuid = UUID.nameUUIDFromBytes(jukebox.getLocation().toString().getBytes());
    if (jukeboxMap.containsKey(uuid)) return;
    PhysicsJukebox physicsJukebox = new PhysicsJukebox();
    jukeboxMap.put(uuid, physicsJukebox);

    plugin.getFoliaLib().getScheduler().runAtLocationTimer(jukebox.getLocation(), task -> {
      physicsJukebox.setTask(task);
      if (task.isCancelled()) return;

      if (!(CustomDiscs.lavaLibExist
        ? (LavaPlayerManager.getInstance().isPlaying(jukebox.getBlock())
           || PlayerManager.getInstance().isPlaying(jukebox.getBlock()))
        : PlayerManager.getInstance().isPlaying(jukebox.getBlock()))) {

        stop(jukebox.getBlock());
        return;
      }

      if (task.isCancelled()) return;

      if (!jukebox.isPlaying()) {
        jukebox.update();
      }

      physicsJukebox.lastUpdateTick = -1; // Может быть... есть вариант получше? Я серьезно!
    }, 1, 20);
  }

  @Data
  public static final class PhysicsJukebox {
    private boolean needUpdate = true;
    private boolean updatedFirst = false;
    private WrappedTask task;
    private long lastUpdateTick = 0;
  }
}
