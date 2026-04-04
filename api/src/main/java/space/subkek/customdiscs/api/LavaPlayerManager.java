package space.subkek.customdiscs.api;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * High-level manager for custom audio playback using LavaPlayer and the Simple Voice Chat API.
 * <p>
 * This manager coordinates audio loading, spatial channel creation, and player
 * synchronization for jukebox-like behavior at specific world coordinates.
 */
public interface LavaPlayerManager {
  /**
   * A handle returned when registering a packet handler.
   * Can be used to unregister a single handler without affecting others registered by the same plugin.
   */
  interface HandlerRegistration {
    /**
     * Removes this handler from the active handler list.
     * After calling this, the associated {@link PacketConsumer} will no longer be invoked.
     */
    void unregister();
  }

  /**
   * A callback invoked for every audio frame before it is sent to the Voice Chat channel.
   * <p>
   * Returning {@code false} drops the frame — it will not be broadcast to any player.
   * All registered consumers are called in registration order; the first {@code false} stops processing.
   */
  @FunctionalInterface
  interface PacketConsumer {
    /**
     * Processes a single audio frame.
     *
     * @param registration The {@link HandlerRegistration} for this consumer, usable for self-removal.
     * @param block        The jukebox block the audio is originating from.
     * @param data         The raw Opus-encoded audio frame data.
     * @return {@code true} to allow the frame to be sent; {@code false} to drop it.
     */
    boolean process(@NotNull HandlerRegistration registration, @NotNull Block block, byte @NotNull [] data);
  }

  /**
   * Registers an audio packet consumer for the given plugin.
   * The consumer will be invoked for every audio frame across all active playback sessions.
   * <p>
   * Multiple consumers may be registered per plugin. They are called in registration order.
   * To remove all consumers registered by a plugin at once, use {@link #unregisterPacketHandlers(Plugin)}.
   *
   * @param plugin   The owning plugin (used for bulk removal).
   * @param consumer The callback to invoke per audio frame.
   */
  void registerPacketHandler(@NotNull Plugin plugin, @NotNull PacketConsumer consumer);

  /**
   * Removes all packet consumers previously registered by the given plugin.
   * Has no effect if the plugin has no registered consumers.
   *
   * @param plugin The plugin whose consumers should be removed.
   */
  void unregisterPacketHandlers(@NotNull Plugin plugin);

  /**
   * Starts audio playback at the specified block location.
   * <p>
   * A {@link LocationalAudioChannel} is created at the block's center position and audio is
   * streamed to all players within the configured jukebox distance. If playback is already
   * active at this block, this method is a no-op.
   *
   * @param block              The block (typically a jukebox) acting as the audio source.
   * @param identifier         The source identifier (URL, file path, or unique ID) used to load the audio.
   * @param actionbarComponent An optional {@link Component} sent to all players in range
   *                           immediately after the channel is created. Pass {@code null} to skip.
   */
  void play(@NotNull Block block, @NotNull String identifier, @Nullable Component actionbarComponent);

  /**
   * Determines if a custom audio track is currently being broadcasted from the given block.
   *
   * @param block The block location to verify.
   * @return {@code true} if an active audio session exists at this location; {@code false} otherwise.
   */
  boolean isPlaying(@NotNull Block block);

  /**
   * Stops audio playback at the specified block and releases all associated resources.
   * <p>
   * This includes closing the {@link LocationalAudioChannel} and terminating
   * the LavaPlayer track.
   *
   * @param block The block where playback should be terminated.
   */
  void stopPlaying(@NotNull Block block);

  /**
   * Terminates all active audio sessions across the entire server.
   * <p>
   * This is typically used during plugin disablement or administrative resets.
   */
  void stopPlayingAll();

  /**
   * Retrieves the spatial audio channel associated with a specific block.
   *
   * @param block The block where playback is occurring.
   * @return The {@link LocationalAudioChannel} being used for broadcast,
   * or {@code null} if no audio is playing at this location.
   */
  @Nullable
  LocationalAudioChannel getAudioChannel(@NotNull Block block);

  /**
   * Retrieves the collection of players who were within the audible radius
   * when the track was initially triggered.
   *
   * @param block The block where playback is occurring.
   * @return A {@link Collection} of {@link ServerPlayer}s who received
   * the initial broadcast, or {@code null} if no session is active.
   */
  @Nullable
  Collection<ServerPlayer> getPlayersInRangeAtStart(@NotNull Block block);
}
