package space.subkek.customdiscs.api;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
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
   * Starts audio playback at the specified block location.
   * <p>
   * This method resolves the {@code sourceURL}, initializes a spatial audio channel,
   * and broadcasts the audio to eligible players.
   *
   * @param block              The block (typically a jukebox) acting as the audio source.
   * @param indetifier         The source identifier (URL, file path, or unique ID) used to load the audio.
   * @param actionbarComponent An optional {@link Component} to display to nearby players
   * upon successful playback initialization.
   */
  void play(@NotNull Block block, @NotNull String indetifier, @Nullable Component actionbarComponent);

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
