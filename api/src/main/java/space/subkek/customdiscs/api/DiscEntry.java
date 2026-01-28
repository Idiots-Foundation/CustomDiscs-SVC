package space.subkek.customdiscs.api;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a custom music disc entry within the system.
 * <p>
 * This class acts as a data container that links a physical Minecraft {@link ItemStack}
 * with its corresponding audio source metadata and identifier.
 */
@SuppressWarnings("ClassCanBeRecord")
public class DiscEntry {
  private final ItemStack disc;
  private final Component name;
  private final String identifier;
  private final boolean local;

  /**
   * Constructs a new DiscEntry.
   *
   * @param disc       The {@link ItemStack} representing the disc in game.
   * @param name       The display name of the track (e.g., used for action bars or tooltips).
   * @param identifier The source identifier (URL, file path, or unique ID) used to load the audio.
   * @param local      {@code true} if the source is stored on the local filesystem,
   * {@code false} if it is a remote resource.
   */
  public DiscEntry(ItemStack disc, Component name, String identifier, boolean local) {
    this.disc = disc;
    this.name = name;
    this.identifier = identifier;
    this.local = local;
  }

  /**
   * Returns the physical item associated with this music disc.
   *
   * @return The {@link ItemStack} of the disc.
   */
  @NotNull
  public ItemStack getDisc() {
    return disc;
  }

  /**
   * Returns the formatted display name of the disc track.
   * Usually displayed to the player when the disc starts playing.
   *
   * @return The track name as an Adventure {@link Component}.
   */
  @NotNull
  public Component getName() {
    return name;
  }

  /**
   * Returns the unique identifier of the audio source.
   * This string is used by the audio engine to resolve and play the track.
   *
   * @return The source string (e.g., a URL or a local file path).
   */
  @NotNull
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Indicates whether the audio source is located on the local server storage.
   *
   * @return {@code true} if the source is local; {@code false} if it is a remote stream.
   */
  public boolean isLocal() {
    return local;
  }
}
