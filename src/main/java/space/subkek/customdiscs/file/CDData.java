package space.subkek.customdiscs.file;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
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

@Getter
@RequiredArgsConstructor
public class CDData {
  private final YamlFile yaml = new YamlFile();
  private final File dataFile;

  private ScheduledTask autosaveTask;

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

  public void save() {
    jukeboxDistanceMap.forEach((uuid, distance) ->
      yaml.set("jukebox.distance." + uuid, distance));

    try {
      yaml.save(dataFile);
    } catch (IOException e) {
      CustomDiscs.error("Error saving data: ", e);
    }
  }

  public void startAutosave() {
    if (autosaveTask != null) throw new IllegalStateException("Autosave data task already exists");
    autosaveTask = CustomDiscs.getPlugin().getSchedulers().async.runAtFixedRate(
      task -> save(),
      60, 60,
      TimeUnit.SECONDS
    );
  }

  public void stopAutosave() {
    autosaveTask.cancel();
    autosaveTask = null;
  }

  public int getJukeboxDistance(Block block) {
    UUID blockUUID = LegacyUtil.getBlockUUID(block);
    return jukeboxDistanceMap.containsKey(blockUUID) ?
      jukeboxDistanceMap.get(blockUUID) : CustomDiscs.getPlugin().getCDConfig().getMusicDiscDistance();
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
