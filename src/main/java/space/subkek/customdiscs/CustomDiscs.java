package space.subkek.customdiscs;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.gson.JsonParser;
import com.tcoded.folialib.FoliaLib;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.subkek.customdiscs.api.CustomDiscsAPI;
import space.subkek.customdiscs.command.CustomDiscsCommand;
import space.subkek.customdiscs.listener.HopperListener;
import space.subkek.customdiscs.listener.JukeboxListener;
import space.subkek.customdiscs.listener.JukeboxPacketListener;
import space.subkek.customdiscs.listener.PlayerListener;
import space.subkek.customdiscs.file.CDConfig;
import space.subkek.customdiscs.file.CDData;
import space.subkek.customdiscs.language.YamlLanguage;
import space.subkek.customdiscs.util.HTTPRequestUtils;

import java.io.File;

public final class CustomDiscs extends JavaPlugin {
  private static Logger logger;
  private static Logger debugLogger;

  @Getter
  private final YamlLanguage language = new YamlLanguage();
  @Getter
  private final File musicData = new File(this.getDataFolder(), "musicdata");
  @Getter
  private final CDConfig cDConfig = new CDConfig(
    new File(this.getDataFolder(), "config.yml"));
  @Getter
  private final CDData cDData = new CDData(
    new File(this.getDataFolder(), "data.yml"));
  @Getter
  private final FoliaLib foliaLib = new FoliaLib(this);
  public int discsPlayed = 0;
  private boolean voicechatAddonRegistered = false;
  private boolean libsLoaded = false;

  public static CustomDiscs getPlugin() {
    return getPlugin(CustomDiscs.class);
  }

  public static void sendMessage(final CommandSender sender, final Component component) {
    sender.sendMessage(component);
  }

  public static void debug(@NotNull final String message, final Object... format) {
    if (getPlugin().getCDConfig().isDebug()) {
      debugLogger.info(message, format);
    }
  }

  public static void info(@NotNull final String message, final Object... format) {
    logger.info(message, format);
  }

  public static void warn(@NotNull final String message, final Object... format) {
    logger.warn(message, format);
  }

  public static void error(@NotNull final String message, @Nullable final Throwable e, final Object... format) {
    logger.error(message, format, e);
  }

  public static void error(@NotNull final String message, final Object... format) {
    logger.error(message, format);
  }

  @Override
  public void onLoad() {
    logger = LoggerFactory.getLogger(this.getName());
    debugLogger = LoggerFactory.getLogger("%s/Debug".formatted(this.getName()));

    this.getServer().getServicesManager().register(
      CustomDiscsAPI.class,
      new CustomDiscsAPIImpl(),
      this,
      ServicePriority.Normal
    );
  }

  @Override
  public void onEnable() {
    this.libsLoaded = System.getProperty("customdiscs.loader.success").equals("true");
    if (!this.libsLoaded) {
      this.getSLF4JLogger().error("Libraries failed to load: Goodbye.");
      this.getServer().getPluginManager().disablePlugin(this);
      return;
    }

    if (this.getDataFolder().mkdir()) CustomDiscs.info("Created plugin data folder");
    if (this.musicData.mkdir()) CustomDiscs.info("Created music data folder");

    this.cDConfig.load();
    this.language.load();
    this.cDData.load();
    this.cDData.startAutosave();

    this.linkBStats();

    this.registerVoicechatHook();

    this.registerEvents();
    this.registerCommands();

    this.foliaLib.getScheduler().runAsync(task -> this.checkUpdates());

    PacketEvents.getAPI().getEventManager().registerListener(
      new JukeboxPacketListener(),
      PacketListenerPriority.HIGHEST
    );
  }

  @Override
  public void onDisable() {
    if (!this.libsLoaded) return;
    LavaPlayerManagerImpl.getInstance().stopPlayingAll();

    this.cDData.stopAutosave();
    this.cDData.save();

    if (this.voicechatAddonRegistered) {
      this.getServer().getServicesManager().unregister(CDVoiceAddon.getInstance());
      CustomDiscs.info("Successfully disabled CustomDiscs plugin");
    }

    this.foliaLib.getScheduler().cancelAllTasks();
  }

  private void registerVoicechatHook() {
    final var service = this.getServer().getServicesManager().load(BukkitVoicechatService.class);

    if (service != null) {
      service.registerPlugin(CDVoiceAddon.getInstance());
      this.voicechatAddonRegistered = true;
      CustomDiscs.info("Successfully enabled voicechat hook");
    } else {
      CustomDiscs.error("Failed to enable voicechat hook");
    }
  }

  private void checkUpdates() {
    try {
      if (!this.cDConfig.isShouldCheckUpdates()) return;
      final var response = HTTPRequestUtils.getTextResponse("https://api.modrinth.com/v2/project/customdiscs-svc/version");

      final var version = JsonParser.parseString(response)
        .getAsJsonArray()
        .get(0)
        .getAsJsonObject()
        .get("version_number")
        .getAsString();

      final var url = "https://modrinth.com/plugin/customdiscs-svc/version/";

      if (!version.equals(getPlugin().getPluginMeta().getVersion())) {
        warn("New version available: {}{}", url, version);

        this.getServer().getPluginManager().registerEvents(new Listener() {
          @EventHandler
          public void onPlayerJoin(final PlayerJoinEvent event) {
            final var player = event.getPlayer();
            if (player.isOp() || player.hasPermission("customdiscs.reload")) {
              sendMessage(player, CustomDiscs.this.getLanguage().PComponent("plugin.messages.update-available", url, version));
            }
          }
        }, this);
      }
    } catch (final Throwable ignore) {
    }
  }

  private void registerCommands() {
    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
      new CustomDiscsCommand().register(event);
    });
  }

  private void registerEvents() {
    this.getServer().getPluginManager().registerEvents(new JukeboxListener(), this);
    this.getServer().getPluginManager().registerEvents(PlayerListener.getInstance(), this);
    this.getServer().getPluginManager().registerEvents(new HopperListener(), this);
  }

  private void linkBStats() {
    final var metrics = new Metrics(this, 20077);

    metrics.addCustomChart(new SimplePie("plugin_language", () -> this.getCDConfig().getLocale()));
    metrics.addCustomChart(new SingleLineChart("discs_played", () -> {
      final var value = this.discsPlayed;
      this.discsPlayed = 0;
      return value;
    }));
  }
}
