package space.subkek.customdiscs.file;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.util.LegacyUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class CDData {
  private final YamlFile yaml = new YamlFile();
  private final File dataFile;

  private WrappedTask autosaveTask;
  private volatile boolean dirty = false;

  private final HashMap<UUID, Integer> jukeboxDistanceMap = new HashMap<>();

  public void load() {
    if (dataFile.exists()) {
      try {
        yaml.load(dataFile);
      } catch (IOException e) {
        CustomDiscs.error("Error while loading config: ", e);
      }
    }

    loadJukeboxDistances();
  }

  public synchronized void save() {
    if (!this.dirty) return;
    jukeboxDistanceMap.forEach((uuid, distance) ->
      yaml.set("jukebox.distance.%s".formatted(uuid), distance));

    try {
      yaml.save(dataFile);
      this.dirty = false;
    } catch (IOException e) {
      CustomDiscs.error("Error saving data: ", e);
    }
  }

  public void startAutosave() {
    if (autosaveTask != null) throw new IllegalStateException("Autosave data task already exists");
    autosaveTask = CustomDiscs.getPlugin().getFoliaLib().getScheduler().runTimerAsync(
      this::save,
      60, 60,
      TimeUnit.SECONDS
    );
  }

  public void stopAutosave() {
    autosaveTask.cancel();
    autosaveTask = null;
    save();
  }

  public int getJukeboxDistance(Block block) {
    UUID blockUUID = LegacyUtil.getBlockUUID(block);
    return jukeboxDistanceMap.containsKey(blockUUID) ?
      jukeboxDistanceMap.get(blockUUID) : CustomDiscs.getPlugin().getCDConfig().getMusicDiscDistance();
  }

  public void setJukeboxDistance(Block block, int distance) {
    UUID blockUUID = LegacyUtil.getBlockUUID(block);
    this.jukeboxDistanceMap.put(blockUUID, distance);
    this.dirty = true;
  }

  private void loadJukeboxDistances() {
    ConfigurationSection section = yaml.getConfigurationSection("jukebox.distance");
    if (section == null) return;

    for (String key : section.getKeys(false)) {
      UUID uuid = UUID.fromString(key);
      int distance = (int) section.get(key);

      jukeboxDistanceMap.put(uuid, distance);
    }
  }
}
