package space.subkek.customdiscs.api.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.subkek.customdiscs.api.DiscEntry;

/**
 * Fired when a custom disc is ejected from a jukebox.
 * <p>
 * This event is cancellable. If cancelled, the disc remains inside the jukebox,
 * and no item is dropped.
 * <p>
 * <strong>Note:</strong> Since this event is synchronous and linked to world state changes,
 * it is safe to perform direct block or inventory modifications here.
 */
public class CustomDiscEjectEvent extends Event implements Cancellable {
  private static final HandlerList HANDLER_LIST = new HandlerList();
  private boolean isCancelled;

  private final Block block;
  private final Player player;
  private final DiscEntry discEntry;

  public CustomDiscEjectEvent(Block block, Player player, DiscEntry discEntry) {
    this.block = block;
    this.player = player;
    this.discEntry = discEntry;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  @Override
  public boolean isCancelled() {
    return this.isCancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.isCancelled = cancel;
  }

  /**
   * Returns the jukebox block involved in the ejection.
   * <p>
   * Because this event is synchronous, it is safe to call any methods on the returned block
   * without additional scheduling.
   *
   * @return The jukebox {@link Block}.
   */
  @NotNull
  public Block getBlock() {
    return block;
  }

  /**
   * Returns the player who initiated the ejection.
   *
   * @return The {@link Player} involved, or {@code null} if triggered by automation
   * (e.g., hoppers or redstone).
   */
  @Nullable
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the metadata of the custom disc being ejected.
   *
   * @return The {@link DiscEntry} containing track and item data.
   */
  @NotNull
  public DiscEntry getDiscEntry() {
    return discEntry;
  }
}
