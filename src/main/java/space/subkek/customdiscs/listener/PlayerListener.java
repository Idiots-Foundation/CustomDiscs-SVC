package space.subkek.customdiscs.listener;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.LavaPlayerManagerImpl;
import space.subkek.customdiscs.api.event.CustomDiscEjectEvent;
import space.subkek.customdiscs.api.event.CustomDiscInsertEvent;
import space.subkek.customdiscs.util.LegacyUtil;
import space.subkek.customdiscs.util.PlayUtil;

import java.util.HashMap;
import java.util.UUID;

public final class PlayerListener implements Listener {
  private static PlayerListener instance;
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  @Getter
  private final HashMap<UUID, Integer> playersSelecting = new HashMap<>();

  public static synchronized PlayerListener getInstance() {
    if (instance == null) return instance = new PlayerListener();
    return instance;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onClickJukebox(final PlayerInteractEvent event) {
    final var playerUUID = event.getPlayer().getUniqueId();
    if (!this.playersSelecting.containsKey(playerUUID)) return;
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
    final var block = event.getClickedBlock();
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) {
      CustomDiscs.sendMessage(event.getPlayer(),
        this.plugin.getLanguage().PComponent("command.distance.messages.error.not-jukebox"));
      this.playersSelecting.remove(playerUUID);
      return;
    }

    event.setCancelled(true);

    final int distance = this.playersSelecting.remove(playerUUID);
    this.plugin.getCDData().setJukeboxDistance(block, distance);

    CustomDiscs.sendMessage(event.getPlayer(), this.plugin.getLanguage().PComponent("command.distance.messages.success", distance));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onInsert(final PlayerInteractEvent event) {
    final var block = event.getClickedBlock();

    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
    if (event.getPlayer().isSneaking()) return;
    if (event.getClickedBlock() == null) return;
    if (event.getItem() == null) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (LegacyUtil.isJukeboxContainsDisc(block)) return;

    if (!LegacyUtil.isCustomDisc(event.getItem())) return;

    CustomDiscs.debug("Jukebox insert by Player event");

    final var discEntry = LegacyUtil.getDiscEntry(event.getItem());

    final var playEvent = new CustomDiscInsertEvent(block, event.getPlayer(), discEntry);
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(playEvent);
    if (!playEvent.isCancelled())
      PlayUtil.play(block, discEntry, playEvent.getPlayer());
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onEject(final PlayerInteractEvent event) {
    final var player = event.getPlayer();
    final var block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (block == null) return;
    if (block.getType() != Material.JUKEBOX) return;
    if (!LegacyUtil.isJukeboxContainsDisc(block)) return;
    final var item = event.getItem() != null ? event.getItem() : new ItemStack(Material.AIR);
    if (player.isSneaking() && item.getType() != Material.AIR) return;
    final var jukebox = (Jukebox) block.getState();
    if (!LegacyUtil.isCustomDisc(jukebox.getRecord())) return;

    CustomDiscs.debug("Jukebox eject by Player event");

    final var stopEvent = new CustomDiscEjectEvent(block, event.getPlayer(), LegacyUtil.getDiscEntry(jukebox.getRecord()));
    CustomDiscs.getPlugin().getServer().getPluginManager().callEvent(stopEvent);

    if (stopEvent.isCancelled()) {
      event.setCancelled(true);
      return;
    }

    LavaPlayerManagerImpl.getInstance().stopPlaying(block);
  }
}
