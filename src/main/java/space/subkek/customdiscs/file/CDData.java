package space.subkek.customdiscs.file;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.util.LegacyUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class CDData {
  private final YamlFile yaml = new YamlFile();
  private final File dataFile;
  private final HashMap<UUID, Integer> jukeboxDistanceMap = new HashMap<>();
  private WrappedTask autosaveTask;
  private volatile boolean dirty = false;

  public void load() {
    if (this.dataFile.exists()) {
      try {
        this.yaml.load(this.dataFile);
      } catch (final IOException e) {
        CustomDiscs.error("Error while loading config: ", e);
      }
    }

    this.loadJukeboxDistances();
  }

  public synchronized void save() {
    if (!this.dirty) return;
    this.jukeboxDistanceMap.forEach((uuid, distance) ->
      this.yaml.set("jukebox.distance.%s".formatted(uuid), distance));

    try {
      this.yaml.save(this.dataFile);
      this.dirty = false;
    } catch (final IOException e) {
      CustomDiscs.error("Error saving data: ", e);
    }
  }

  public void startAutosave() {
    if (this.autosaveTask != null) throw new IllegalStateException("Autosave data task already exists");
    this.autosaveTask = CustomDiscs.getPlugin().getFoliaLib().getScheduler().runTimerAsync(
      this::save,
      60, 60,
      TimeUnit.SECONDS
    );
  }

  public void stopAutosave() {
    this.autosaveTask.cancel();
    this.autosaveTask = null;
    this.save();
  }

  public int getJukeboxDistance(final Block block) {
    final var blockUUID = LegacyUtil.getBlockUUID(block);
    return this.jukeboxDistanceMap.containsKey(blockUUID) ?
      this.jukeboxDistanceMap.get(blockUUID) : CustomDiscs.getPlugin().getCDConfig().getMusicDiscDistance();
  }

  public void setJukeboxDistance(final Block block, final int distance) {
    final var blockUUID = LegacyUtil.getBlockUUID(block);
    this.jukeboxDistanceMap.put(blockUUID, distance);
    this.dirty = true;
  }

  private void loadJukeboxDistances() {
    final var section = this.yaml.getConfigurationSection("jukebox.distance");
    if (section == null) return;

    for (final var key : section.getKeys(false)) {
      final var uuid = UUID.fromString(key);
      final var distance = (int) section.get(key);

      this.jukeboxDistanceMap.put(uuid, distance);
    }
  }
}
