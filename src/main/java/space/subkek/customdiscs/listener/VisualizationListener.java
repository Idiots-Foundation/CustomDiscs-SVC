package space.subkek.customdiscs.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import space.subkek.customdiscs.CustomDiscs;

public final class VisualizationListener implements Listener {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  @EventHandler
  public void onJoin(final PlayerJoinEvent event) {
    this.resendNextTick(event.getPlayer());
  }

  @EventHandler
  public void onRespawn(final PlayerRespawnEvent event) {
    this.resendNextTick(event.getPlayer());
  }

  @EventHandler
  public void onWorldChange(final PlayerChangedWorldEvent event) {
    this.resendNextTick(event.getPlayer());
  }

  private void resendNextTick(final Player player) {
    this.plugin.getFoliaLib().getScheduler().runAtEntityLater(
      player,
      () -> this.plugin.getVisualizationManager().resend(player),
      1
    );
  }
}
