package space.subkek.customdiscs;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import space.subkek.customdiscs.api.CustomDiscsAPI;
import space.subkek.customdiscs.api.LavaPlayerManager;
import space.subkek.customdiscs.util.LegacyUtil;

public class CustomDiscsAPIImpl implements CustomDiscsAPI {
  @Override
  public @NotNull LavaPlayerManager getLavaPlayerManager() {
    return LavaPlayerManagerImpl.getInstance();
  }

  @Override
  public boolean isCustomDisc(@NotNull ItemStack item) {
    return LegacyUtil.isCustomDisc(item);
  }
}
