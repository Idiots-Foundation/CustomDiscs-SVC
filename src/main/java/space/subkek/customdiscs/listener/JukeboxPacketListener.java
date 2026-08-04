package space.subkek.customdiscs.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import space.subkek.customdiscs.LavaPlayerManagerImpl;

public final class JukeboxPacketListener implements PacketListener {
  @Override
  public void onPacketSend(@NonNull final PacketSendEvent event) {
    if (event.getPacketType() != PacketType.Play.Server.EFFECT) return;

    final var packet = new WrapperPlayServerEffect(event);
    if (packet.getType() != 1010) return;

    final var position = packet.getPosition();
    final var world = ((Player) event.getPlayer()).getWorld();
    final var block = world.getBlockAt(position.getX(), position.getY(), position.getZ());

    if (LavaPlayerManagerImpl.getInstance().isPlaying(block)) {
      event.setCancelled(true);
    }
  }
}
