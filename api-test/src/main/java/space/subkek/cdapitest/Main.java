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
import org.jetbrains.annotations.NotNull;
import space.subkek.customdiscs.api.CustomDiscsAPI;
import space.subkek.customdiscs.api.LavaPlayerManager;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.api.event.CustomDiscInsertEvent;
import space.subkek.customdiscs.api.event.LavaPlayerStopPlayingEvent;

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
        player.sendPlainMessage("Starting last LavaPlayer at %s".formatted(player.getLocation()));
        api.getLavaPlayerManager().play(player.getLocation().getBlock(), lastIdentifier, MINIMESSAGE.deserialize("<red>LAST IDENTIFIER"));

        return Command.SINGLE_SUCCESS;
      }))
      .then(Commands.literal("stopall").executes(ctx -> {
        CommandSender sender = ctx.getSource().getSender();

        sender.sendPlainMessage("Stopping all LavaPlayers on the server");
        api.getLavaPlayerManager().stopPlayingAll();

        return Command.SINGLE_SUCCESS;
      }));

    registerStutterHandler();

    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(root.build()));
  }

  @Override
  public void onDisable() {
    api.getLavaPlayerManager().unregisterPacketHandlers(this);
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
  public void discStopped(LavaPlayerStopPlayingEvent event) {
    broadcast("<red>Disc stopped at %s.formatted(jukebox destroyed: %b".formatted(event.getBlock().getLocation(), event.getBlock().getType() != Material.JUKEBOX));
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
    broadcast("<green>Disc %s inserted by %s at %s".formatted(PLAINTEXT.serialize(event.getDiscEntry().getName()), inserter, event.getBlock().getLocation()));
  }

  @EventHandler
  public void discEjected(CustomDiscEjectEvent event) {
    Player player = event.getPlayer();
    String inserter = player != null ? player.getName() : "Hopper";
    broadcast("<yellow>Disc %s ejected by %s at %s".formatted(PLAINTEXT.serialize(event.getDiscEntry().getName()), inserter, event.getBlock().getLocation()));
  }

  private void registerStutterHandler() {
    api.getLavaPlayerManager().registerPacketHandler(this, new LavaPlayerManager.PacketConsumer() {
      long counter = 0;
      long last_time = Long.MIN_VALUE;
      boolean isMuted = false;

      @Override
      public boolean process(LavaPlayerManager.@NotNull HandlerRegistration handler, @NotNull Block block, byte @NotNull [] data) {
        if (this.last_time == Long.MIN_VALUE) this.last_time = System.currentTimeMillis();
        else if (System.currentTimeMillis() >= this.last_time + 2000) {
          this.isMuted = !this.isMuted;
          this.last_time = System.currentTimeMillis();

          var sps = api.getLavaPlayerManager().getPlayersInRangeAtStart(block);
          if (sps != null) {
            sps.forEach(sp -> {
              if (sp.getPlayer() instanceof Player player && player.isOnline())
                player.sendMessage(MINIMESSAGE.deserialize("<light_purple>It's not lag but an API test %d".formatted(counter)));
            });
          }

          counter++;
        }

        if (counter >= 8) handler.unregister();

        return !isMuted;
      }
    });
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
