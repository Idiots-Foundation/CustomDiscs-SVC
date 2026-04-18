package space.subkek.customdiscs.file;

import lombok.Getter;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.language.Language;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@Getter
@SuppressWarnings("unused")
public class CDConfig {
  private final YamlFile yaml = new YamlFile();
  private final File configFile;
  private String configVersion;

  public CDConfig(File configFile) {
    this.configFile = configFile;
  }

  public void load() {
    if (configFile.exists()) {
      try {
        yaml.load(configFile);
      } catch (IOException e) {
        CustomDiscs.error("Error loading file: ", e);
      }
    }

    configVersion = getString("info.version", "1.5", "Don't change this value");
    setComment("info",
      "CustomDiscs Configuration",
      "Join our Discord for support: https://discord.gg/eRvwvmEXWz");
    debug = getBoolean("global.debug", false);

    switch (configVersion) {
      case "1.0":
        migrateTo1_1();
      case "1.1":
        migrateTo1_2();
      case "1.2":
        migrateTo1_3();
      case "1.3":
        migrateTo1_4();
      case "1.4":
        migrateTo1_5();
    }

    for (Method method : this.getClass().getDeclaredMethods()) {
      if (Modifier.isPrivate(method.getModifiers()) &&
        method.getReturnType().equals(Void.TYPE) &&
        method.getName().endsWith("Settings")
      ) {
        try {
          method.invoke(this);
        } catch (Throwable t) {
          CustomDiscs.error("Failed to load configuration option from {}", t, method.getName());
        }
      }
    }

    save();
  }

  public void save() {
    try {
      yaml.save(configFile);
    } catch (IOException e) {
      CustomDiscs.error("Error saving file: ", e);
    }
  }

  private void setComment(String key, String... comment) {
    if (yaml.contains(key) && comment.length > 0) {
      yaml.setComment(key, String.join("\n", comment), CommentType.BLOCK);
    }
  }

  private void ensureDefault(String key, Object defaultValue, String... comment) {
    if (!yaml.contains(key))
      yaml.set(key, defaultValue);

    setComment(key, comment);
  }

  private boolean getBoolean(String key, boolean defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getBoolean(key, defaultValue);
  }

  private int getInt(String key, int defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getInt(key, defaultValue);
  }

  private double getDouble(String key, double defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getDouble(key, defaultValue);
  }

  private String getString(String key, String defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getString(key, defaultValue);
  }

  private List<String> getStringList(String key, List<String> defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getStringList(key);
  }

  private String locale = Language.ENGLISH.getLabel();
  private boolean shouldCheckUpdates = true;
  private boolean debug = false;

  private void globalSettings() {
    locale = getString("global.locale", locale, "Language of the plugin",
      """
        Supported: %s
        Unknown languages will be replaced with %s""".formatted(Language.getAllSeparatedComma(), Language.ENGLISH.getLabel()
      )
    );
    if (!Language.isExists(locale)) locale = Language.ENGLISH.getLabel();
    shouldCheckUpdates = getBoolean("global.check-updates", shouldCheckUpdates);
    debug = getBoolean("global.debug", debug);
  }

  private int maxDownloadSize = 50;
  private int localCustomModelData = 0;
  private List<String> remoteTabComplete = List.of("https://www.youtube.com/watch?v=", "https://soundcloud.com/");
  private int remoteCustomModelDataYoutube = 0;
  private String remoteFilterYoutube = "https?:\\/\\/(?:www\\.youtube\\.com\\/watch\\?v=|youtu\\.be\\/).+";
  private int remoteCustomModelDataSoundcloud = 0;
  private String remoteFilterSoundcloud = "https?:\\/\\/soundcloud\\.com\\/[^\\s]+";
  private int distanceCommandMaxDistance = 64;

  private void commandSettings() {
    maxDownloadSize = getInt("command.download.max-size", maxDownloadSize,
      "The maximum download size in megabytes.");
    localCustomModelData = getInt("command.create.local.custom-model", localCustomModelData);
    remoteTabComplete = getStringList("command.create.remote.tabcomplete", remoteTabComplete);
    remoteCustomModelDataYoutube = getInt("command.create.remote.youtube.custom-model", remoteCustomModelDataYoutube);
    remoteFilterYoutube = getString("command.create.remote.youtube.filter", remoteFilterYoutube);
    remoteCustomModelDataSoundcloud = getInt("command.create.remote.soundcloud.custom-model", remoteCustomModelDataSoundcloud);
    remoteFilterSoundcloud = getString("command.create.remote.soundcloud.filter", remoteFilterSoundcloud);
    distanceCommandMaxDistance = getInt("command.distance.max", distanceCommandMaxDistance);

    setComment("command.create.remote.tabcomplete", """
      tabcomplete — Displaying hints when entering remote command
      filter — Regex filter for applying custom-model-data to remote disk""");
  }

  private int musicDiscDistance = 64;
  private float musicDiscVolume = 1f;
  private boolean allowHoppers = true;

  private void discSettings() {
    musicDiscDistance = getInt("disc.distance", musicDiscDistance,
      "The distance from which music discs can be heard in blocks.");
    musicDiscVolume = Float.parseFloat(getString("disc.volume", String.valueOf(musicDiscVolume),
      "The master volume of music discs from 0-1.", "You can set values like 0.5 for 50% volume."
    ));
    allowHoppers = getBoolean("disc.allow-hoppers", allowHoppers, "Please ensure that in the config/paper-world-defaults.yaml the value hopper.disable-move-event is false");
  }

  private boolean youtubeOauth2 = false;
  private String youtubePoToken = "";
  private String youtubePoVisitorData = "";
  private String youtubeRemoteServer = "";
  private String youtubeRemoteServerPassword = "";
  private String youtubeHttpProxy = "";

  private void providersSettings() {
    youtubeHttpProxy = getString("providers.youtube.http-proxy", youtubeHttpProxy,
      "HTTP/HTTPS proxy for LavaPlayer.",
      "Format: [scheme://][user:pass@]host:port",
      "http://user:password@ip:port",
      "https://user:password@ip:port"
    );

    youtubeOauth2 = getBoolean("providers.youtube.use-oauth2", youtubeOauth2, """
      This may help if the plugin is not working properly.
      When you first play the disc after the server starts, you will see an authorization request in the console. Use a secondary account for security purposes.""");

    youtubePoToken = getString("providers.youtube.po-token.token", youtubePoToken);
    youtubePoVisitorData = getString("providers.youtube.po-token.visitor-data", youtubePoVisitorData);

    setComment("providers.youtube.po-token", """
      If you have oauth2 enabled, leave these fields blank.
      This may help if the plugin is not working properly.
      https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-potoken""");

    youtubeRemoteServer = getString("providers.youtube.remote-server.url", youtubeRemoteServer);
    youtubeRemoteServerPassword = getString("providers.youtube.remote-server.password", youtubeRemoteServerPassword);

    setComment("providers.youtube.remote-server", """
      A method for obtaining streaming via a remote server that emulates a web client.
      Make sure Oauth2 was enabled!
      https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-remote-cipher-server""");
  }

  private void setConfigVersion(String version) {
    yaml.set("info.version", version);
    configVersion = version;
  }

  private void removeValue(String key) {
    if (yaml.contains(key)) {
      yaml.remove(key);
      CustomDiscs.debug("Config successfully removed value {}", key);
      return;
    }
    CustomDiscs.debug("Config not found value {} to remove", key);
  }

  private void migrateValue(String key, String newKey) {
    if (yaml.contains(key)) {
      Object value = yaml.get(key);
      yaml.remove(key);
      yaml.set(newKey, value);
      CustomDiscs.debug("Config successfully migrated value {} to {}", key, newKey);
      return;
    }
    CustomDiscs.debug("Config not found value {} to migrate to {}", key, newKey);
  }

  private void migrateTo1_1() {
    CustomDiscs.debug("Config migrating from v1.0 to v1.1");
    migrateValue("music-disc-distance", "disc.distance");
    migrateValue("music-disc-volume", "disc.volume");
    migrateValue("max-download-size", "command.download.max-size");
    migrateValue("custom-model-data.enable", "command.create.custom-model-data.enable");
    migrateValue("custom-model-data.value", "command.create.custom-model-data.value");
    removeValue("custom-model-data");
    removeValue("providers.youtube.email");
    removeValue("providers.youtube.password");
    migrateValue("locale", "global.locale");
    migrateValue("debug", "global.debug");
    removeValue("cleaning-disc");
    setConfigVersion("1.1");
  }

  private void migrateTo1_2() {
    CustomDiscs.debug("Config migrating from v1.1 to v1.2");
    removeValue("providers.youtube.po-token.auto");
    setConfigVersion("1.2");
  }

  private void migrateTo1_3() {
    CustomDiscs.debug("Config migrating from v1.2 to v1.3");
    removeValue("command.create.custom-model-data");
    removeValue("command.createyt");
    removeValue("command.createsc");
    setConfigVersion("1.3");
  }

  private void migrateTo1_4() {
    CustomDiscs.debug("Config migrating from v1.3 to v1.4");
    removeValue("debug");
    setConfigVersion("1.4");
  }

  private void migrateTo1_5() {
    removeValue("command.create.remote.youtube.filter");
    removeValue("command.create.remote.soundcloud.filter");
    setConfigVersion("1.5");
  }
}
