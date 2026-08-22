package space.subkek.customdiscs.file;

import lombok.Getter;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.file.YamlFile;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.language.Language;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

@Getter
public final class CDConfig {
  private final YamlFile yaml = new YamlFile();
  private final File configFile;
  private String configVersion;

  public CDConfig(final File configFile) {
    this.configFile = configFile;
  }

  public void load() {
    if (this.configFile.exists()) {
      try {
        this.yaml.load(this.configFile);
      } catch (final IOException e) {
        CustomDiscs.error("Error loading file: ", e);
      }
    }

    this.configVersion = this.getString("info.version", "1.6", "Don't change this value");
    this.setComment("info",
      "CustomDiscs Configuration",
      "Join our Discord for support: https://discord.gg/eRvwvmEXWz");
    this.debug = this.getBoolean("global.debug", false);

    switch (this.configVersion) {
      case "1.0":
        this.migrateTo1_1();
      case "1.1":
        this.migrateTo1_2();
      case "1.2":
        this.migrateTo1_3();
      case "1.3":
        this.migrateTo1_4();
      case "1.4":
        this.migrateTo1_5();
      case "1.5":
        this.migrateTo1_6();
    }

    for (final var method : this.getClass().getDeclaredMethods()) {
      if (Modifier.isPrivate(method.getModifiers()) &&
        method.getReturnType().equals(Void.TYPE) &&
        method.getName().endsWith("Settings")
      ) {
        try {
          method.invoke(this);
        } catch (final Throwable t) {
          CustomDiscs.error("Failed to load configuration option from {}", t, method.getName());
        }
      }
    }

    this.save();
  }

  public void save() {
    try {
      this.yaml.save(this.configFile);
    } catch (final IOException e) {
      CustomDiscs.error("Error saving file: ", e);
    }
  }

  private void setComment(final String key, final String... comment) {
    if (this.yaml.contains(key) && comment.length > 0) {
      this.yaml.setComment(key, String.join("\n", comment), CommentType.BLOCK);
    }
  }

  private void ensureDefault(final String key, final Object defaultValue, final String... comment) {
    if (!this.yaml.contains(key))
      this.yaml.set(key, defaultValue);

    this.setComment(key, comment);
  }

  private boolean getBoolean(final String key, final boolean defaultValue, final String... comment) {
    this.ensureDefault(key, defaultValue, comment);
    return this.yaml.getBoolean(key, defaultValue);
  }

  private int getInt(final String key, final int defaultValue, final String... comment) {
    this.ensureDefault(key, defaultValue, comment);
    return this.yaml.getInt(key, defaultValue);
  }

  private double getDouble(final String key, final double defaultValue, final String... comment) {
    this.ensureDefault(key, defaultValue, comment);
    return this.yaml.getDouble(key, defaultValue);
  }

  private String getString(final String key, final String defaultValue, final String... comment) {
    this.ensureDefault(key, defaultValue, comment);
    return this.yaml.getString(key, defaultValue);
  }

  private List<String> getStringList(final String key, final List<String> defaultValue, final String... comment) {
    this.ensureDefault(key, defaultValue, comment);
    return this.yaml.getStringList(key);
  }

  private String locale = Language.ENGLISH.getLabel();
  private boolean shouldCheckUpdates = true;
  private boolean debug = false;

  private void globalSettings() {
    this.locale = this.getString("global.locale", this.locale, "Language of the plugin",
      """
        Supported: %s
        Unknown languages will be replaced with %s""".formatted(Language.getAllSeparatedComma(), Language.ENGLISH.getLabel()
      )
    );
    if (!Language.isExists(this.locale)) this.locale = Language.ENGLISH.getLabel();
    this.shouldCheckUpdates = this.getBoolean("global.check-updates", this.shouldCheckUpdates);
    this.debug = this.getBoolean("global.debug", this.debug);
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
    this.maxDownloadSize = this.getInt("command.download.max-size", this.maxDownloadSize,
      "The maximum download size in megabytes.");
    this.localCustomModelData = this.getInt("command.create.local.custom-model", this.localCustomModelData);
    this.remoteTabComplete = this.getStringList("command.create.remote.tabcomplete", this.remoteTabComplete);
    this.remoteCustomModelDataYoutube = this.getInt("command.create.remote.youtube.custom-model", this.remoteCustomModelDataYoutube);
    this.remoteFilterYoutube = this.getString("command.create.remote.youtube.filter", this.remoteFilterYoutube);
    this.remoteCustomModelDataSoundcloud = this.getInt("command.create.remote.soundcloud.custom-model", this.remoteCustomModelDataSoundcloud);
    this.remoteFilterSoundcloud = this.getString("command.create.remote.soundcloud.filter", this.remoteFilterSoundcloud);
    this.distanceCommandMaxDistance = this.getInt("command.distance.max", this.distanceCommandMaxDistance);

    this.setComment("command.create.remote.tabcomplete", """
      tabcomplete — Displaying hints when entering remote command
      filter — Regex filter for applying custom-model-data to remote disk""");
  }

  private int musicDiscDistance = 64;
  private float musicDiscVolume = 1f;
  private int maxTrackLengthSeconds = 1200;
  private VisualizationMode visualizationMode = VisualizationMode.PARTICLES;
  private int hologramDistance = 16;
  private int hologramNameMaxLength = 32;
  private HologramPositionMode hologramPositionMode = HologramPositionMode.STATIC;
  private double hologramOffsetX = 0d;
  private double hologramOffsetY = 1.2d;
  private double hologramOffsetZ = 0d;

  private void discSettings() {
    this.musicDiscDistance = this.getInt("disc.distance", this.musicDiscDistance,
      "The distance from which music discs can be heard in blocks.");
    this.musicDiscVolume = Float.parseFloat(this.getString("disc.volume", String.valueOf(this.musicDiscVolume),
      "The master volume of music discs from 0-1.", "You can set values like 0.5 for 50% volume."
    ));
    this.maxTrackLengthSeconds = this.getInt("disc.max-track-length-seconds", this.maxTrackLengthSeconds,
      "Maximum track length in seconds. Set to 0 to disable the limit and allow streams.");
    if (this.maxTrackLengthSeconds < 0) {
      CustomDiscs.warn("Invalid negative maximum track length {}; falling back to 1200", this.maxTrackLengthSeconds);
      this.maxTrackLengthSeconds = 1200;
    }

    final var configuredMode = this.getString("disc.visualization.mode", "particles",
      "Supported visualization modes: particles, hologram, both, off.");
    try {
      this.visualizationMode = VisualizationMode.valueOf(configuredMode.trim().toUpperCase(Locale.ROOT));
    } catch (final RuntimeException e) {
      CustomDiscs.warn("Invalid disc visualization mode '{}'; falling back to particles", configuredMode);
      this.visualizationMode = VisualizationMode.PARTICLES;
    }
    this.removeValue("disc.visualization.hologram.text");

    this.hologramDistance = this.getInt("disc.visualization.hologram.distance", this.hologramDistance,
      "The maximum distance in blocks from which the hologram is rendered.",
      "This distance is exact only when the client's Entity Distance setting is 100%.");
    if (this.hologramDistance <= 0 || this.hologramDistance > 64) {
      CustomDiscs.warn("hologram distance is Invalid(zero/negative/above client's default tracking range (64)); falling back to 16");
      this.hologramDistance = 16;
    }

    this.hologramNameMaxLength = this.getInt(
      "disc.visualization.hologram.name-max-length",
      this.hologramNameMaxLength,
      "Maximum visible disc name length, including the truncation suffix."
    );
    if (this.hologramNameMaxLength < 1) {
      CustomDiscs.warn("Invalid hologram name length {}; falling back to 32", this.hologramNameMaxLength);
      this.hologramNameMaxLength = 32;
    }

    final var configuredPositionMode = this.getString(
      "disc.visualization.hologram.position.mode",
      "rotational",
      "Supported hologram position modes: static, rotational.",
      "Static faces the side where playback was activated. Rotational allows player's client to rotate hologram."
    );
    try {
      this.hologramPositionMode = HologramPositionMode.valueOf(configuredPositionMode.trim().toUpperCase(Locale.ROOT));
    } catch (final RuntimeException e) {
      CustomDiscs.warn("Invalid hologram position mode '{}'; falling back to rotational", configuredPositionMode);
      this.hologramPositionMode = HologramPositionMode.ROTATIONAL;
    }

    this.hologramOffsetX = this.getFiniteDouble("disc.visualization.hologram.position.offset.x", 0d,
      "Sideways offset relative to the direction the hologram faces.");
    this.hologramOffsetY = this.getFiniteDouble("disc.visualization.hologram.position.offset.y", 1.2d,
      "Vertical offset above the jukebox.");
    this.hologramOffsetZ = this.getFiniteDouble("disc.visualization.hologram.position.offset.z", 0d,
      "Forward offset relative to the direction the hologram faces.");
  }

  private double getFiniteDouble(final String key, final double defaultValue, final String... comment) {
    final var value = this.getDouble(key, defaultValue, comment);
    if (Double.isFinite(value)) return value;

    CustomDiscs.warn("Invalid hologram offset at '{}'; falling back to {}", key, defaultValue);
    return defaultValue;
  }

  private boolean youtubeOauth2 = false;
  private String youtubePoToken = "";
  private String youtubePoVisitorData = "";
  private String youtubeRemoteServer = "";
  private String youtubeRemoteServerPassword = "";
  private String youtubeHttpProxy = "";

  private void providersSettings() {
    this.youtubeHttpProxy = this.getString("providers.youtube.http-proxy", this.youtubeHttpProxy,
      "HTTP/HTTPS proxy for LavaPlayer.",
      "Format: [scheme://][user:pass@]host:port",
      "http://user:password@ip:port",
      "https://user:password@ip:port"
    );

    this.youtubeOauth2 = this.getBoolean("providers.youtube.use-oauth2", this.youtubeOauth2, """
      This may help if the plugin is not working properly.
      When you first play the disc after the server starts, you will see an authorization request in the console. Use a secondary account for security purposes.""");

    this.youtubePoToken = this.getString("providers.youtube.po-token.token", this.youtubePoToken);
    this.youtubePoVisitorData = this.getString("providers.youtube.po-token.visitor-data", this.youtubePoVisitorData);

    this.setComment("providers.youtube.po-token", """
      If you have oauth2 enabled, leave these fields blank.
      This may help if the plugin is not working properly.
      https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-potoken""");

    this.youtubeRemoteServer = this.getString("providers.youtube.remote-server.url", this.youtubeRemoteServer);
    this.youtubeRemoteServerPassword = this.getString("providers.youtube.remote-server.password", this.youtubeRemoteServerPassword);

    this.setComment("providers.youtube.remote-server", """
      A method for obtaining streaming via a remote server that emulates a web client.
      Make sure Oauth2 was enabled!
      https://github.com/lavalink-devs/youtube-source?tab=readme-ov-file#using-a-remote-cipher-server""");
  }

  private void setConfigVersion(final String version) {
    this.yaml.set("info.version", version);
    this.configVersion = version;
  }

  private void removeValue(final String key) {
    if (this.yaml.contains(key)) {
      this.yaml.remove(key);
      CustomDiscs.debug("Config successfully removed value {}", key);
      return;
    }
    CustomDiscs.debug("Config not found value {} to remove", key);
  }

  private void migrateValue(final String key, final String newKey) {
    if (this.yaml.contains(key)) {
      final var value = this.yaml.get(key);
      this.yaml.remove(key);
      this.yaml.set(newKey, value);
      CustomDiscs.debug("Config successfully migrated value {} to {}", key, newKey);
      return;
    }
    CustomDiscs.debug("Config not found value {} to migrate to {}", key, newKey);
  }

  private void migrateTo1_1() {
    CustomDiscs.debug("Config migrating from v1.0 to v1.1");
    this.migrateValue("music-disc-distance", "disc.distance");
    this.migrateValue("music-disc-volume", "disc.volume");
    this.migrateValue("max-download-size", "command.download.max-size");
    this.migrateValue("custom-model-data.enable", "command.create.custom-model-data.enable");
    this.migrateValue("custom-model-data.value", "command.create.custom-model-data.value");
    this.removeValue("custom-model-data");
    this.removeValue("providers.youtube.email");
    this.removeValue("providers.youtube.password");
    this.migrateValue("locale", "global.locale");
    this.migrateValue("debug", "global.debug");
    this.removeValue("cleaning-disc");
    this.setConfigVersion("1.1");
  }

  private void migrateTo1_2() {
    CustomDiscs.debug("Config migrating from v1.1 to v1.2");
    this.removeValue("providers.youtube.po-token.auto");
    this.setConfigVersion("1.2");
  }

  private void migrateTo1_3() {
    CustomDiscs.debug("Config migrating from v1.2 to v1.3");
    this.removeValue("command.create.custom-model-data");
    this.removeValue("command.createyt");
    this.removeValue("command.createsc");
    this.setConfigVersion("1.3");
  }

  private void migrateTo1_4() {
    CustomDiscs.debug("Config migrating from v1.3 to v1.4");
    this.removeValue("debug");
    this.setConfigVersion("1.4");
  }

  private void migrateTo1_5() {
    CustomDiscs.debug("Config migrating from v1.4 to v1.5");
    this.removeValue("command.create.remote.youtube.filter");
    this.removeValue("command.create.remote.soundcloud.filter");
    this.setConfigVersion("1.5");
  }

  private void migrateTo1_6() {
    CustomDiscs.debug("Config migrating from v1.5 to v1.6");
    this.removeValue("disc.allow-hoppers");
    this.setConfigVersion("1.6");
  }

  public enum VisualizationMode {
    PARTICLES,
    HOLOGRAM,
    BOTH,
    OFF
  }

  public enum HologramPositionMode {
    STATIC,
    ROTATIONAL
  }
}
