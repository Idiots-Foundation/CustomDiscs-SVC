package space.subkek.customdiscs.api;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The primary entry point for the CustomDiscs API.
 * Provides access to the audio playback manager and disc identification utilities.
 */
public interface CustomDiscsAPI {
  /**
   * Retrieves the instance of the CustomDiscsAPI from the Bukkit Services Manager.
   *
   * @return The API instance, or null if the plugin is not loaded.
   */
  @Nullable
  static CustomDiscsAPI get() {
    RegisteredServiceProvider<CustomDiscsAPI> rsp = Bukkit.getServicesManager().getRegistration(CustomDiscsAPI.class);
    if (rsp == null) return null;
    return rsp.getProvider();
  }

  /**
   * @return The interface to get simple access to the LavaPlayerManager.
   */
  @NotNull
  LavaPlayerManager getLavaPlayerManager();

  /**
   * Checks if the provided ItemStack is registered as a custom music disc.
   *
   * @param item The item to check.
   * @return true if the item is a valid custom disc, false otherwise.
   */
  boolean isCustomDisc(@NotNull ItemStack item);
}
