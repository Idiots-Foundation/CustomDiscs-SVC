package io.github.subkek.customdiscs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.tcoded.folialib.FoliaLib;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import dev.jorel.commandapi.CommandAPI;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import io.github.subkek.customdiscs.event.HopperHandler;
import io.github.subkek.customdiscs.event.JukeboxHandler;
import io.github.subkek.customdiscs.event.PlayerHandler;
import io.github.subkek.customdiscs.file.CDConfig;
import io.github.subkek.customdiscs.file.CDData;
import io.github.subkek.customdiscs.language.YamlLanguage;
import io.github.subkek.customdiscs.library.AssetsDownloader;
import io.github.subkek.customdiscs.metrics.BStatsLink;
import io.github.subkek.customdiscs.util.Formatter;
import io.github.subkek.customdiscs.util.HTTPRequestUtils;
import io.github.subkek.customdiscs.util.LegacyUtil;
import io.github.subkek.customdiscs.util.TaskScheduler;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Jukebox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CustomDiscs extends JavaPlugin {
  public static final String PLUGIN_ID = "customdiscs";
  public static final String LIBRARY_ID = "lavaplayer-lib";
  @Getter
  private final YamlLanguage language = new YamlLanguage();
  @Getter
  private final CDConfig cDConfig = new CDConfig(
      new File(getDataFolder().getPath(), "config.yml"));
  @Getter
  private final CDData cDData = new CDData(
      new File(getDataFolder().getPath(), "data.yml"));
  @Getter
  private final FoliaLib foliaLib = new FoliaLib(this);
  @Getter
  private TaskScheduler scheduler;
  public int discsPlayed = 0;
  private boolean voicechatAddonRegistered = false;
  public static boolean lavaLibExist = false;

  public static CustomDiscs getPlugin() {
    return getPlugin(CustomDiscs.class);
  }

  @Override
  public void onEnable() {
    CommandAPI.onEnable();

    scheduler = new TaskScheduler(1);
    scheduler.setLogger(CustomDiscs::error);

    if (getDataFolder().mkdir()) CustomDiscs.info("Created plugin data folder");

    cDConfig.init();
    language.init();
    cDData.load();
    cDData.startAutosave();

    AssetsDownloader.loadLibraries(getDataFolder());
    linkBStats();

    File musicData = new File(this.getDataFolder(), "musicdata");
    if (!(musicData.exists())) {
      if (musicData.mkdir()) CustomDiscs.info("Created music data folder");
    }

    try {
      Class.forName("io.github.subkek.lavaplayer.libs.dev.lavalink.youtube.YoutubeAudioSourceManager");
      lavaLibExist = true;
      CustomDiscs.info("{0} installed, youtube support enabled", LIBRARY_ID);
    } catch (ClassNotFoundException e) {
      CustomDiscs.warn("{0} not installed, youtube support disabled: https://github.com/Idiots-Foundation/lavaplayer-lib/releases", LIBRARY_ID);
    }

    registerVoicechatHook();

    registerEvents();
    registerCommands();

    foliaLib.getScheduler().runAsync(task -> checkUpdate());

    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
      @Override
      public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getIntegers().read(0).equals(1010)) {
          Jukebox jukebox = (Jukebox) packet.getBlockPositionModifier().read(0).toLocation(event.getPlayer().getWorld()).getBlock().getState();

          if (!jukebox.getRecord().hasItemMeta()) return;

          if (LegacyUtil.isCustomDisc(jukebox.getRecord()) ||
              LegacyUtil.isCustomStreamingDisc(jukebox.getRecord())) {
            event.setCancelled(true);

            PhysicsManager.getInstance().start(jukebox);
          }
        }
      }
    });
  }

  @Override
  public void onDisable() {
    CommandAPI.onDisable();

    if (lavaLibExist) LavaPlayerManager.getInstance().stopPlayingAll();
    PlayerManager.getInstance().stopPlayingAll();

    cDData.stopAutosave();
    cDData.save();

    if (voicechatAddonRegistered) {
      getServer().getServicesManager().unregister(CDVoiceAddon.getInstance());
      CustomDiscs.info("Successfully disabled CustomDiscs plugin");
    }

    scheduler.shutdown();
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

  private void checkUpdate() {
    String url = "https://modrinth.com/plugin/customdiscs-svc/version/";

    try {
      String version = (String) ((JSONObject)
          ((JSONArray) new JSONParser().parse(
              HTTPRequestUtils.getTextResponse(
                  "https://api.modrinth.com/v2/project/customdiscs-svc/version"
              )
          )).get(0)
      ).get("version_number");

      if (!version.equals(getPlugin().getPluginMeta().getVersion())) {
        warn("New version available: {0}{1}", url, version);

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
    } catch (Exception ignore) {
    }
  }

  private void registerCommands() {
    new CustomDiscsCommand().register("customdiscs");
  }

  private void registerEvents() {
    getServer().getPluginManager().registerEvents(new JukeboxHandler(), this);
    getServer().getPluginManager().registerEvents(PlayerHandler.getInstance(), this);
    if (getCDConfig().isAllowHoppers())
      getServer().getPluginManager().registerEvents(HopperHandler.getInstance(), this);
  }

  private void linkBStats() {
    BStatsLink bstats = new BStatsLink(getPlugin(), 20077);

    bstats.addCustomChart(new BStatsLink.SimplePie("plugin_language", () -> getCDConfig().getLocale()));
    bstats.addCustomChart(new BStatsLink.SingleLineChart("discs_played", () -> {
      int value = discsPlayed;
      discsPlayed = 0;
      return value;
    }));
  }

  public static void sendMessage(CommandSender sender, Component component) {
    sender.sendMessage(component);
  }

  public static void debug(@NotNull String message, Object... format) {
    if (!getPlugin().getCDConfig().isDebug()) return;
    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}",
                getPlugin().getLanguage().string("prefix.debug"),
                Formatter.format(message, format)
            )
        )
    );
  }

  public static void info(@NotNull String message, Object... format) {
    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}",
                getPlugin().getLanguage().string("prefix.info"),
                Formatter.format(message, format)
            )
        )
    );
  }

  private static String getStackTraceString(Throwable e) {
    String stackTrace = "";

    if (e != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      e.printStackTrace(pw);
      stackTrace = sw.getBuffer().toString();
    }

    return stackTrace;
  }

  public static void warn(@NotNull String message, @Nullable Throwable e, Object... format) {
    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}{2}",
                getPlugin().getLanguage().string("prefix.warn"),
                Formatter.format(message, format),
                getStackTraceString(e)
            )
        )
    );
  }

  public static void warn(@NotNull String message, Object... format) {
    warn(message, null, format);
  }

  public static void error(@NotNull String message, @Nullable Throwable e, Object... format) {
    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}{2}",
                getPlugin().getLanguage().string("prefix.error"),
                Formatter.format(message, format),
                getStackTraceString(e)
            )
        )
    );
  }

  public static void error(@NotNull String message, Object... format) {
    error(message, null, format);
  }
}
