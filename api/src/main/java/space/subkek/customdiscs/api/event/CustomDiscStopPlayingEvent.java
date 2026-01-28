package space.subkek.customdiscs.api.event;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired whenever a custom disc stops playing, whether stopped by a player or finishing naturally.
 * <p>
 * <strong>Note:</strong> This event may be triggered asynchronously or from a specific region thread.
 * Any modifications to the world or calls to non-thread-safe Bukkit methods must be
 * wrapped in an appropriate scheduler task (e.g., Global or Region scheduler).
 */
public class CustomDiscStopPlayingEvent extends Event {
  private static final HandlerList HANDLER_LIST = new HandlerList();

  private final Block block;
  private final String identifier;

  public CustomDiscStopPlayingEvent(Block block, String identifier) {
    this.block = block;
    this.identifier = identifier;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  /**
   * Returns the block where the music was playing.
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
   * @return The identifier (URL or local path) of the track that stopped.
   */
  @NotNull
  public String getIdentifier() {
    return identifier;
  }
}
