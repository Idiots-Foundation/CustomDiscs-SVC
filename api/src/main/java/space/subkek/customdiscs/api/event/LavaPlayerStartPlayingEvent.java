package space.subkek.customdiscs.api.event;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever a lavaplayer starts playing.
 * <p>
 * <strong>Note:</strong> This event may be triggered asynchronously or from a specific region thread.
 * Any modifications to the world or calls to non-thread-safe Bukkit methods must be
 * wrapped in an appropriate scheduler task (e.g., Global or Region scheduler).
 */
public class LavaPlayerStartPlayingEvent extends Event implements Cancellable {
  private static final HandlerList HANDLER_LIST = new HandlerList();
  private boolean isCancelled;

  private final Block block;
  private final String identifier;

  public LavaPlayerStartPlayingEvent(Block block, String identifier) {
    super(true);
    this.block = block;
    this.identifier = identifier;

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
   * Returns the block where the music starts playing.
   * <p>
   * <strong>Thread Safety Warning:</strong> Since this event may be fired asynchronously,
   * you must ensure thread safety when interacting with the returned block.
   * Always verify the block state (e.g., {@code block.getType()}) within a synchronized
   * context or scheduler task, as the block may have been modified or destroyed
   * before the event reached the listener.
   *
   * @return The block where the audio originated.
   */
  @NotNull
  public Block getBlock() {
    return block;
  }

  /**
   * Returns the identifier of the track that is starting to play.
   *
   * @return The source identifier (URL or local file path) of the track.
   */
  @NotNull
  public String getIdentifier() {
    return identifier;
  }
}
