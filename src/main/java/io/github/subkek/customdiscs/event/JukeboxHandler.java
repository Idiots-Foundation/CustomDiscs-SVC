package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import io.github.subkek.customdiscs.util.LegacyUtil;
import io.github.subkek.customdiscs.util.PlayUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class JukeboxHandler implements Listener {
  private static ItemStack getItemStack(PlayerInteractEvent event, Player player) {
    ItemStack itemInvolvedInEvent;
    if (event.getMaterial().equals(Material.AIR)) {

      if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
        itemInvolvedInEvent = player.getInventory().getItemInMainHand();
      } else if (!player.getInventory().getItemInOffHand().getType().equals(Material.AIR)) {
        itemInvolvedInEvent = player.getInventory().getItemInOffHand();
      } else {
        itemInvolvedInEvent = new ItemStack(Material.AIR);
      }

    } else {
      itemInvolvedInEvent = new ItemStack(event.getMaterial());
    }
    return itemInvolvedInEvent;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInsert(PlayerInteractEvent event) {
    Block block = event.getClickedBlock();

    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
    if (event.getPlayer().isSneaking()) return;
    if (event.getClickedBlock() == null) return;
    if (event.getItem() == null) return;
    if (!event.getItem().hasItemMeta()) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (LegacyUtil.isJukeboxContainsDisc(block)) return;

    boolean isCustomDisc = LegacyUtil.isCustomDisc(event.getItem());
    boolean isCustomStreamingDisc = LegacyUtil.isCustomStreamingDisc(event.getItem());

    if (!isCustomDisc && !isCustomStreamingDisc) return;

    CustomDiscs.debug("Jukebox insert by Player event");

    if (isCustomDisc)
      PlayUtil.playStandard(block, event.getItem());

    if (isCustomStreamingDisc) {
      if (!CustomDiscs.lavaLibExist) {
        CustomDiscs.error(CustomDiscs.LIBRARY_ID + " is not installed. Direct streaming support impossible! https://github.com/Idiots-Foundation/lavaplayer-lib/releases");
        return;
      }

      PlayUtil.playLava(block, event.getItem());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEject(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!LegacyUtil.isJukeboxContainsDisc(block)) return;
    ItemStack itemInvolvedInEvent = getItemStack(event, player);
    if (player.isSneaking() && !itemInvolvedInEvent.getType().equals(Material.AIR)) return;
    Jukebox jukebox = (Jukebox) block.getState();
    if (!LegacyUtil.isCustomDisc(jukebox.getRecord()) &&
        !LegacyUtil.isCustomStreamingDisc(jukebox.getRecord())) return;

    CustomDiscs.debug("Jukebox eject by Player event");

    PlayerManager.getInstance().stopPlaying(block);
    if (CustomDiscs.lavaLibExist) LavaPlayerManager.getInstance().stopPlaying(block);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJukeboxBreak(BlockBreakEvent event) {

    Block block = event.getBlock();

    if (block.getType() != Material.JUKEBOX) return;

    PlayerManager.getInstance().stopPlaying(block);
    if (CustomDiscs.lavaLibExist) LavaPlayerManager.getInstance().stopPlaying(block);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJukeboxExplode(EntityExplodeEvent event) {
    for (Block explodedBlock : event.blockList()) {
      if (explodedBlock.getType() == Material.JUKEBOX) {
        PlayerManager.getInstance().stopPlaying(explodedBlock);
        LavaPlayerManager.getInstance().stopPlaying(explodedBlock);
      }
    }
  }
}
