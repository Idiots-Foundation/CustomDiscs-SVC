package space.subkek.customdiscs;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import com.google.gson.JsonParser;
import com.tcoded.folialib.FoliaLib;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import dev.jorel.commandapi.CommandAPI;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.subkek.customdiscs.api.CustomDiscsAPI;
import space.subkek.customdiscs.command.CustomDiscsCommand;
import space.subkek.customdiscs.event.HopperHandler;
import space.subkek.customdiscs.event.JukeboxHandler;
import space.subkek.customdiscs.event.PlayerHandler;
import space.subkek.customdiscs.file.CDConfig;
import space.subkek.customdiscs.file.CDData;
import space.subkek.customdiscs.language.YamlLanguage;
import space.subkek.customdiscs.util.HTTPRequestUtils;

import java.io.File;

public class CustomDiscs extends JavaPlugin {
  private static Logger logger;
  private static Logger debugLogger;

  @Getter
  private final YamlLanguage language = new YamlLanguage();
  @Getter
  private final File musicData = new File(this.getDataFolder(), "musicdata");
  @Getter
  private final CDConfig cDConfig = new CDConfig(
    new File(getDataFolder(), "config.yml"));
  @Getter
  private final CDData cDData = new CDData(
    new File(getDataFolder(), "data.yml"));
  public int discsPlayed = 0;
  private boolean voicechatAddonRegistered = false;
  private boolean libsLoaded = false;
  @Getter
  private final FoliaLib foliaLib = new FoliaLib(this);

  public static CustomDiscs getPlugin() {
    return getPlugin(CustomDiscs.class);
  }

  @Override
  public void onLoad() {
    logger = LoggerFactory.getLogger(this.getName());
    debugLogger = LoggerFactory.getLogger("%s/Debug".formatted(this.getName()));

    getServer().getServicesManager().register(
      CustomDiscsAPI.class,
      new CustomDiscsAPIImpl(),
      this,
      ServicePriority.Normal
    );
  }

  @Override
  public void onEnable() {
    libsLoaded = System.getProperty("customdiscs.loader.success").equals("true");
    if (!libsLoaded) {
      getSLF4JLogger().error("Libraries failed to load: Goodbye.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    CommandAPI.onEnable();

    if (getDataFolder().mkdir()) CustomDiscs.info("Created plugin data folder");
    if (musicData.mkdir()) CustomDiscs.info("Created music data folder");

    cDConfig.load();
    language.load();
    cDData.load();
    cDData.startAutosave();

    linkBStats();

    registerVoicechatHook();

    registerEvents();
    registerCommands();

    foliaLib.getScheduler().runAsync(task -> checkUpdates());

    PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
      @Override
      public void onPacketSend(@NonNull PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.EFFECT) {
          var packet = new WrapperPlayServerEffect(event);

          if (packet.getType() == 1010) {
            var pos = packet.getPosition();
            var player = (org.bukkit.entity.Player) event.getPlayer();
            var world = player.getWorld();

            var block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());

            if (LavaPlayerManagerImpl.getInstance().isPlaying(block)) {
              event.setCancelled(true);
            }
          }
        }
      }
    }, PacketListenerPriority.HIGHEST);
  }

  @Override
  public void onDisable() {
    if (!libsLoaded) return;
    CommandAPI.onDisable();
    LavaPlayerManagerImpl.getInstance().stopPlayingAll();

    cDData.stopAutosave();
    cDData.save();

    if (voicechatAddonRegistered) {
      getServer().getServicesManager().unregister(CDVoiceAddon.getInstance());
      CustomDiscs.info("Successfully disabled CustomDiscs plugin");
    }

    foliaLib.getScheduler().cancelAllTasks();
  }

  private void registerVoicechatHook() {
    BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

    if (service != null) {
      service.registerPlugin(CDVoiceAddon.getInstance());
      voicechatAddonRegistered = true;
      CustomDiscs.info("Successfully enabled voicechat hook");
    } else {
      CustomDiscs.error("Failed to enable voicechat hook");
    }
  }

  private void checkUpdates() {
    try {
      if (!cDConfig.isShouldCheckUpdates()) return;
      String response = HTTPRequestUtils.getTextResponse("https://api.modrinth.com/v2/project/customdiscs-svc/version");

      String version = JsonParser.parseString(response)
        .getAsJsonArray()
        .get(0)
        .getAsJsonObject()
        .get("version_number")
        .getAsString();

      String url = "https://modrinth.com/plugin/customdiscs-svc/version/";

      if (!version.equals(getPlugin().getPluginMeta().getVersion())) {
        warn("New version available: {}{}", url, version);

        getServer().getPluginManager().registerEvents(new Listener() {
          @EventHandler
          public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            if (player.isOp() || player.hasPermission("customdiscs.reload")) {
              sendMessage(player, getLanguage().PComponent("plugin.messages.update-available", url, version));
            }
          }
        }, this);
      }
    } catch (Throwable ignore) {
    }
  }

  private void registerCommands() {
    new CustomDiscsCommand().register("customdiscs");
  }

  private void registerEvents() {
    getServer().getPluginManager().registerEvents(new JukeboxHandler(), this);
    getServer().getPluginManager().registerEvents(PlayerHandler.getInstance(), this);
    getServer().getPluginManager().registerEvents(new HopperHandler(), this);
  }

  private void linkBStats() {
    Metrics metrics = new Metrics(this, 20077);

    metrics.addCustomChart(new SimplePie("plugin_language", () -> getCDConfig().getLocale()));
    metrics.addCustomChart(new SingleLineChart("discs_played", () -> {
      int value = discsPlayed;
      discsPlayed = 0;
      return value;
    }));
  }

  public static void sendMessage(CommandSender sender, Component component) {
    sender.sendMessage(component);
  }

  public static void debug(@NotNull String message, Object... format) {
    if (getPlugin().getCDConfig().isDebug()) {
      debugLogger.info(message, format);
    }
  }

  public static void info(@NotNull String message, Object... format) {
    logger.info(message, format);
  }

  public static void warn(@NotNull String message, Object... format) {
    logger.warn(message, format);
  }

  public static void error(@NotNull String message, @Nullable Throwable e, Object... format) {
    logger.error(message, format, e);
  }

  public static void error(@NotNull String message, Object... format) {
    logger.error(message, format);
  }
}
