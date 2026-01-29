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
 * Fired when a custom disc is inserted into a jukebox.
 * <p>
 * This event can be triggered by a player's manual interaction or by automation
 * (e.g., a hopper or a dispenser). If cancelled, the disc will not be placed
 * inside the jukebox, and the playback will not initiate.
 */
public class CustomDiscInsertEvent extends Event implements Cancellable {
  private static final HandlerList HANDLER_LIST = new HandlerList();
  private boolean isCancelled;

  private final Block block;
  private final Player player;
  private final DiscEntry discEntry;

  public CustomDiscInsertEvent(Block block, Player player, DiscEntry discEntry) {
    this.block = block;
    this.player = player;
    this.discEntry = discEntry;

    this.isCancelled = false;
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
   * Returns the jukebox block where the disc is being inserted.
   *
   * @return The jukebox {@link Block}.
   */
  @NotNull
  public Block getBlock() {
    return block;
  }

  /**
   * Returns the player who inserted the disc.
   *
   * @return The {@link Player} involved, or {@code null} if the insertion
   * was triggered by automation (e.g., a hopper or a dispenser).
   */
  @Nullable
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the disc metadata entry being inserted into the jukebox.
   *
   * @return The {@link DiscEntry} containing track and item data.
   */
  @NotNull
  public DiscEntry getDiscEntry() {
    return discEntry;
  }
}
