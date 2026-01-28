package space.subkek.customdiscs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import dev.jorel.commandapi.CommandAPI;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.block.Jukebox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.subkek.customdiscs.api.CustomDiscsAPI;
import space.subkek.customdiscs.command.CustomDiscsCommand;
import space.subkek.customdiscs.event.HopperHandler;
import space.subkek.customdiscs.event.JukeboxHandler;
import space.subkek.customdiscs.event.PlayerHandler;
import space.subkek.customdiscs.file.CDConfig;
import space.subkek.customdiscs.file.CDData;
import space.subkek.customdiscs.language.YamlLanguage;
import space.subkek.customdiscs.util.Formatter;
import space.subkek.customdiscs.util.HTTPRequestUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

@SuppressWarnings("UnstableApiUsage")
public class CustomDiscs extends JavaPlugin {
  public static final String PLUGIN_ID = "customdiscs";

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
  private final Schedulers schedulers = new Schedulers(this);

  public static CustomDiscs getPlugin() {
    return getPlugin(CustomDiscs.class);
  }

  @Override
  public void onLoad() {
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

    if (getDataFolder().mkdir()) getSLF4JLogger().info("Created plugin data folder");

    cDConfig.init();
    language.init();
    cDData.load();
    cDData.startAutosave();

    linkBStats();

    if (!(musicData.exists())) {
      if (musicData.mkdir()) CustomDiscs.info("Created music data folder");
    }

    registerVoicechatHook();

    registerEvents();
    registerCommands();

    schedulers.async.runNow(task -> startingChecks());

    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
      @Override
      public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getIntegers().read(0).equals(1010)) {
          Jukebox jukebox = (Jukebox) packet.getBlockPositionModifier().read(0).toLocation(event.getPlayer().getWorld()).getBlock().getState();
          if (LavaPlayerManagerImpl.getInstance().isPlaying(jukebox.getBlock())) {
            event.setCancelled(true);
            jukebox.stopPlaying();
            ParticleManager.start(jukebox.getBlock());
          }
        }
      }
    });
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

    schedulers.async.cancelTasks();
    schedulers.global.cancelTasks();
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

  private void startingChecks() {
    String url = "https://modrinth.com/plugin/customdiscs-svc/version/";

    try {
      String response = HTTPRequestUtils.getTextResponse("https://api.modrinth.com/v2/project/customdiscs-svc/version");

      String version = com.google.gson.JsonParser.parseString(response)
          .getAsJsonArray()
          .get(0)
          .getAsJsonObject()
          .get("version_number")
          .getAsString();

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
