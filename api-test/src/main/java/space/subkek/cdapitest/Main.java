package space.subkek.cdapitest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.maxhenkel.voicechat.api.ServerPlayer;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import space.subkek.customdiscs.api.CustomDiscsAPI;
import space.subkek.customdiscs.api.LavaPlayerManager;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.api.event.CustomDiscInsertEvent;
import space.subkek.customdiscs.api.event.CustomDiscStopPlayingEvent;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends JavaPlugin implements Listener {
  private String lastIdentifier = null;
  private CustomDiscsAPI api;

  @Override
  public void onEnable() {
    api = CustomDiscsAPI.get();

    getServer().getPluginManager().registerEvents(this, this);

    LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cdapitest")
      .requires(ctx -> ctx.getSender() instanceof Player)
      .then(Commands.literal("playlast").executes(ctx -> {
        Player player = (Player) ctx.getSource().getSender();

        if (lastIdentifier == null) {
          player.sendPlainMessage("No played discs before");
          return Command.SINGLE_SUCCESS;
        }
        player.sendPlainMessage(String.format("Starting last LavaPlayer at %s", player.getLocation()));
        api.getLavaPlayerManager().play(player.getLocation().getBlock(), lastIdentifier, MINIMESSAGE.deserialize("LAST IDENTIFIER"));

        return Command.SINGLE_SUCCESS;
      }))
      .then(Commands.literal("stopall").executes(ctx -> {
        CommandSender sender = ctx.getSource().getSender();

        sender.sendPlainMessage("Stopping all LavaPlayers on the server");
        api.getLavaPlayerManager().stopPlayingAll();

        return Command.SINGLE_SUCCESS;
      }));

    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(root.build()));
  }

  private final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();
  private final PlainTextComponentSerializer PLAINTEXT = PlainTextComponentSerializer.plainText();

  private void broadcast(String message) {
    Component component = MINIMESSAGE.deserialize(message);
    for (var player : getServer().getOnlinePlayers()) {
      player.sendMessage(component);
    }
  }

  @EventHandler
  public void discStopped(CustomDiscStopPlayingEvent event) {
    broadcast(String.format("<red>Disc stopped at %s, jukebox destroyed: %b", event.getBlock().getLocation(), event.getBlock().getType() != Material.JUKEBOX));
    Collection<ServerPlayer> sps = api.getLavaPlayerManager().getPlayersInRangeAtStart(event.getBlock());
    if (sps == null) {
      broadcast("<red>WTF!? Players is null");
      return;
    }
    sps.forEach(sp -> {
      Player player = (Player) sp.getPlayer();
      if (player.isOnline()) player.sendMessage(MINIMESSAGE.deserialize("<gold>Wow disc is stopped."));
    });
  }

  @EventHandler
  public void discInserted(CustomDiscInsertEvent event) {
    Player player = event.getPlayer();
    String inserter = player != null ? player.getName() : "Hopper or Dropper";
    lastIdentifier = event.getDiscEntry().getIdentifier();
    broadcast(String.format("<green>Disc %s inserted by %s at %s", PLAINTEXT.serialize(event.getDiscEntry().getName()), inserter, event.getBlock().getLocation()));
  }

  @EventHandler
  public void discEjected(CustomDiscEjectEvent event) {
    Player player = event.getPlayer();
    String inserter = player != null ? player.getName() : "Hopper";
    broadcast(String.format("<yellow>Disc %s ejected by %s at %s", PLAINTEXT.serialize(event.getDiscEntry().getName()), inserter, event.getBlock().getLocation()));
  }

  // Example of 24/7 lobby music in every world
  private final Set<Chunk> chunks = ConcurrentHashMap.newKeySet();

  @EventHandler
  public void worldLoadEvent(ChunkLoadEvent event) {
    //noinspection PointlessBooleanExpression
    if (!false) return;

    if (event.getChunk().getX() != 0 || event.getChunk().getZ() != 0) return;

    World world = event.getWorld();
    LavaPlayerManager lpm = api.getLavaPlayerManager();

    File musicFile = new File("plugins/CustomDiscs/musicdata/", "lp.mp3");
    if (!musicFile.exists()) return;

    if (chunks.add(event.getChunk())) {
      getServer().getRegionScheduler().runAtFixedRate(this, world, 0, 0, task -> {
        Block block = world.getBlockAt(0, 64, 0);

        if (!lpm.isPlaying(block))
          api.getLavaPlayerManager().play(block, musicFile.getPath(), MINIMESSAGE.deserialize("<gold>LP3she4ka for you"));
      }, 20, 20);
    }
  }
}
