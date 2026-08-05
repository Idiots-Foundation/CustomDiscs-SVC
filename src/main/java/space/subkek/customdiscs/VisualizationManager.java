package space.subkek.customdiscs;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import space.subkek.customdiscs.file.CDConfig.HologramPositionMode;
import space.subkek.customdiscs.file.CDConfig.VisualizationMode;
import space.subkek.customdiscs.util.LegacyUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VisualizationManager {
  private static final float ENTITY_TRACKING_RANGE = 64f;
  private static final int VIEWER_DISCOVERY_PERIOD = 20;
  private static final int ROTATIONAL_UPDATE_PERIOD = 2;
  private static final int STATIC_UPDATE_PERIOD = 20;
  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
  private static final List<String> PLACEHOLDERS = List.of("name", "length", "played", "remaining", "percentage");

  private final CustomDiscs plugin;
  private final Map<UUID, ActiveVisualization> activeVisualizations = new HashMap<>();

  public VisualizationManager(final CustomDiscs plugin) {
    this.plugin = plugin;
  }

  public synchronized void start(final Block block, final Component song, final AudioTrack track) {
    final var id = LegacyUtil.getBlockUUID(block);
    final var previous = this.activeVisualizations.remove(id);
    if (previous != null) this.destroyVisual(previous);

    final var visualization = new ActiveVisualization(block, song, track);
    this.activeVisualizations.put(id, visualization);
    try {
      this.createVisual(visualization);
    } catch (final RuntimeException e) {
      this.destroyVisual(visualization);
      CustomDiscs.error("Failed to create jukebox visualization: ", e);
    }
  }

  public synchronized void stop(final Block block) {
    final var visualization = this.activeVisualizations.remove(LegacyUtil.getBlockUUID(block));
    if (visualization != null) this.destroyVisual(visualization);
  }

  public synchronized void reload() {
    for (final var visualization : this.activeVisualizations.values()) {
      this.destroyVisual(visualization);
      try {
        this.createVisual(visualization);
      } catch (final RuntimeException e) {
        this.destroyVisual(visualization);
        CustomDiscs.error("Failed to recreate jukebox visualization: ", e);
      }
    }
  }

  public synchronized void resend(final Player player) {
    for (final var visualization : this.activeVisualizations.values()) {
      if (visualization.entityId == null || !visualization.block.getWorld().equals(player.getWorld())) continue;
      try {
        this.showHologramIfActive(player, visualization, true);
      } catch (final RuntimeException e) {
        CustomDiscs.error("Failed to resend jukebox hologram to {}: ", e, player.getName());
      }
    }
  }

  public synchronized void shutdown() {
    for (final var visualization : this.activeVisualizations.values()) {
      this.destroyVisual(visualization);
    }
    this.activeVisualizations.clear();
  }

  private void createVisual(final ActiveVisualization visualization) {
    final var mode = this.plugin.getCDConfig().getVisualizationMode();
    if (mode == VisualizationMode.PARTICLES) {
      visualization.particleTask = ParticleManager.start(visualization.block);
    } else if (mode == VisualizationMode.HOLOGRAM) {
      visualization.entityId = reserveEntityId();
      visualization.viewerDiscoveryTask = this.plugin.getFoliaLib().getScheduler().runAtLocationTimer(
        visualization.block.getLocation(),
        () -> this.discoverViewers(visualization),
        1,
        VIEWER_DISCOVERY_PERIOD
      );
    }
  }

  @SuppressWarnings("deprecation")
  private static int reserveEntityId() {
    return Bukkit.getUnsafe().nextEntityId();
  }

  private synchronized void discoverViewers(final ActiveVisualization visualization) {
    if (visualization.entityId == null ||
      this.activeVisualizations.get(LegacyUtil.getBlockUUID(visualization.block)) != visualization) return;

    final var center = visualization.block.getLocation().add(0.5d, 0.5d, 0.5d);
    final var distance = this.plugin.getCDConfig().getHologramDistance();
    for (final var player : visualization.block.getWorld().getNearbyPlayers(center, distance)) {
      if (visualization.viewerTasks.containsKey(player.getUniqueId())) continue;
      this.plugin.getFoliaLib().getScheduler().runAtEntity(
        player,
        task -> this.showHologramIfActive(player, visualization, false)
      );
    }
  }

  private synchronized void showHologramIfActive(final Player player, final ActiveVisualization visualization,
                                                  final boolean force) {
    if (this.activeVisualizations.get(LegacyUtil.getBlockUUID(visualization.block)) != visualization) return;
    if (visualization.entityId == null || !this.isViewerInRange(player, visualization)) {
      this.removeViewer(player, visualization);
      return;
    }
    if (!force && visualization.viewerTasks.containsKey(player.getUniqueId())) return;
    this.showHologram(player, visualization);
  }

  private void destroyVisual(final ActiveVisualization visualization) {
    if (visualization.particleTask != null) {
      visualization.particleTask.cancel();
      visualization.particleTask = null;
    }

    if (visualization.viewerDiscoveryTask != null) {
      visualization.viewerDiscoveryTask.cancel();
      visualization.viewerDiscoveryTask = null;
    }

    if (visualization.entityId != null) {
      final var destroyPacket = new WrapperPlayServerDestroyEntities(visualization.entityId);
      for (final var entry : visualization.viewerTasks.entrySet()) {
        entry.getValue().cancel();
        final var player = this.plugin.getServer().getPlayer(entry.getKey());
        if (player != null && player.isOnline()) {
          try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
          } catch (final RuntimeException e) {
            CustomDiscs.error("Failed to destroy jukebox hologram for {}: ", e, player.getName());
          }
        }
      }
      visualization.entityId = null;
      visualization.viewerTasks.clear();
      visualization.rotationalTicks.clear();
    }
  }

  private void showHologram(final Player player, final ActiveVisualization visualization) {
    final int entityId = visualization.entityId;
    final var packetManager = PacketEvents.getAPI().getPlayerManager();
    final var previousTask = visualization.viewerTasks.remove(player.getUniqueId());
    if (previousTask != null) {
      previousTask.cancel();
      visualization.rotationalTicks.remove(player.getUniqueId());
      packetManager.sendPacket(player, new WrapperPlayServerDestroyEntities(entityId));
    }

    final var pose = this.hologramPose(player, visualization);
    packetManager.sendPacket(player, new WrapperPlayServerSpawnEntity(
      entityId,
      UUID.randomUUID(),
      EntityTypes.TEXT_DISPLAY,
      new com.github.retrooper.packetevents.protocol.world.Location(
        pose.position.x,
        pose.position.y,
        pose.position.z,
        pose.yaw,
        pose.pitch
      ),
      0,
      0,
      Vector3d.zero()
    ));
    packetManager.sendPacket(player, new WrapperPlayServerEntityMetadata(entityId, this.hologramMetadata(visualization)));

    final var positionMode = this.plugin.getCDConfig().getHologramPositionMode();
    final var period = positionMode == HologramPositionMode.ROTATIONAL
      ? ROTATIONAL_UPDATE_PERIOD
      : STATIC_UPDATE_PERIOD;
    final var task = this.plugin.getFoliaLib().getScheduler().runAtEntityTimer(
      player,
      () -> this.updateViewer(player, visualization),
      period,
      period
    );
    visualization.viewerTasks.put(player.getUniqueId(), task);
  }

  private synchronized void updateViewer(final Player player, final ActiveVisualization visualization) {
    if (visualization.entityId == null ||
      this.activeVisualizations.get(LegacyUtil.getBlockUUID(visualization.block)) != visualization ||
      !this.isViewerInRange(player, visualization)) {
      this.removeViewer(player, visualization);
      return;
    }

    try {
      final var packetManager = PacketEvents.getAPI().getPlayerManager();
      if (this.plugin.getCDConfig().getHologramPositionMode() == HologramPositionMode.ROTATIONAL) {
        final var pose = this.hologramPose(player, visualization);
        packetManager.sendPacket(player, new WrapperPlayServerEntityTeleport(
          visualization.entityId,
          pose.position,
          pose.yaw,
          pose.pitch,
          false
        ));
        visualization.rotationalTicks.merge(player.getUniqueId(), ROTATIONAL_UPDATE_PERIOD, Integer::sum);
        if (visualization.rotationalTicks.get(player.getUniqueId()) < STATIC_UPDATE_PERIOD) return;
        visualization.rotationalTicks.put(player.getUniqueId(), 0);
      }

      packetManager.sendPacket(player, new WrapperPlayServerEntityMetadata(
        visualization.entityId,
        this.hologramMetadata(visualization)
      ));
    } catch (final RuntimeException e) {
      CustomDiscs.error("Failed to update jukebox hologram for {}: ", e, player.getName());
    }
  }

  private boolean isViewerInRange(final Player player, final ActiveVisualization visualization) {
    if (!player.isOnline() || !visualization.block.getWorld().equals(player.getWorld())) return false;

    final var center = visualization.block.getLocation().add(0.5d, 0.5d, 0.5d);
    final var distance = this.plugin.getCDConfig().getHologramDistance();
    return player.getLocation().distanceSquared(center) <= (double) distance * distance;
  }

  private void removeViewer(final Player player, final ActiveVisualization visualization) {
    final var task = visualization.viewerTasks.remove(player.getUniqueId());
    if (task != null) task.cancel();
    visualization.rotationalTicks.remove(player.getUniqueId());
    if (task == null || visualization.entityId == null || !player.isOnline()) return;

    PacketEvents.getAPI().getPlayerManager().sendPacket(
      player,
      new WrapperPlayServerDestroyEntities(visualization.entityId)
    );
  }

  private ArrayList<EntityData<?>> hologramMetadata(final ActiveVisualization visualization) {
    final var metadata = new ArrayList<EntityData<?>>();
    metadata.add(new EntityData<>(15, EntityDataTypes.BYTE,
      this.plugin.getCDConfig().getHologramPositionMode() == HologramPositionMode.STATIC ? (byte) 3 : (byte) 0));
    metadata.add(new EntityData<>(17, EntityDataTypes.FLOAT,
      this.plugin.getCDConfig().getHologramDistance() / ENTITY_TRACKING_RANGE));
    metadata.add(new EntityData<>(23, EntityDataTypes.ADV_COMPONENT, this.hologramText(visualization)));
    metadata.add(new EntityData<>(25, EntityDataTypes.INT, 0));
    metadata.add(new EntityData<>(27, EntityDataTypes.BYTE, (byte) 0x01));
    return metadata;
  }

  private Component hologramText(final ActiveVisualization visualization) {
    final var duration = visualization.track.getDuration();
    final var unknownDuration = visualization.track.getInfo().isStream || duration <= 0L || duration == Units.DURATION_MS_UNKNOWN;
    final var played = unknownDuration
      ? Math.max(0L, visualization.track.getPosition())
      : Math.clamp(visualization.track.getPosition(), 0L, duration);
    final var name = this.formattedName(visualization.song);
    final var playedComponent = this.formattedTime("played", played);
    final Component length;
    final Component remainingComponent;
    final Component percentageComponent;
    if (unknownDuration) {
      length = MINI_MESSAGE.deserialize(this.plugin.getLanguage().string("hologram.formats.length-unknown"));
      remainingComponent = MINI_MESSAGE.deserialize(this.plugin.getLanguage().string("hologram.formats.remaining-unknown"));
      percentageComponent = MINI_MESSAGE.deserialize(this.plugin.getLanguage().string("hologram.formats.percentage-unknown"));
    } else {
      final var remaining = duration - played;
      final var percentage = played >= duration
        ? 100L
        : Math.min(99L, (long) Math.floor(played * 100d / duration));
      length = this.formattedTime("length", duration);
      remainingComponent = this.formattedTime("remaining", remaining);
      percentageComponent = MINI_MESSAGE.deserialize(
        this.plugin.getLanguage().string("hologram.formats.percentage").replace("{value}", "<value>"),
        Placeholder.unparsed("value", Long.toString(percentage))
      );
    }

    final var lines = this.plugin.getLanguage().strings("hologram.lines").stream()
      .map(this::normalizePlaceholders)
      .map(line -> MINI_MESSAGE.deserialize(
        line,
        Placeholder.component("name", name),
        Placeholder.component("length", length),
        Placeholder.component("played", playedComponent),
        Placeholder.component("remaining", remainingComponent),
        Placeholder.component("percentage", percentageComponent)
      ))
      .toList();
    return Component.join(JoinConfiguration.newlines(), lines);
  }

  private Component formattedName(final Component song) {
    final var maxLength = this.plugin.getCDConfig().getHologramNameMaxLength();
    final var plainName = PLAIN_TEXT.serialize(song);
    final var truncated = plainName.codePointCount(0, plainName.length()) > maxLength;
    final var value = truncated ? truncate(song, Math.max(0, maxLength - 1)) : song;
    final var formatKey = truncated ? "hologram.formats.name-truncated" : "hologram.formats.name";
    return MINI_MESSAGE.deserialize(
      this.plugin.getLanguage().string(formatKey).replace("{value}", "<value>"),
      Placeholder.component("value", value)
    );
  }

  private Component formattedTime(final String placeholder, final long milliseconds) {
    final var totalSeconds = Math.max(0L, milliseconds / 1000L);
    final var minutes = totalSeconds / 60L;
    final var seconds = totalSeconds % 60L;
    return MINI_MESSAGE.deserialize(
      this.plugin.getLanguage().string("hologram.formats.%s".formatted(placeholder))
        .replace("{minutes}", "<minutes>")
        .replace("{seconds}", "<seconds>"),
      Placeholder.unparsed("minutes", Long.toString(minutes)),
      Placeholder.unparsed("seconds", "%02d".formatted(seconds))
    );
  }

  private String normalizePlaceholders(final String line) {
    var normalized = line;
    for (final var placeholder : PLACEHOLDERS) {
      normalized = normalized.replace("{%s}".formatted(placeholder), "<%s>".formatted(placeholder));
    }
    return normalized;
  }

  private HologramPose hologramPose(final Player player, final ActiveVisualization visualization) {
    final var config = this.plugin.getCDConfig();
    final var blockLocation = visualization.block.getLocation();
    final var anchorX = blockLocation.getX() + 0.5d;
    final var anchorY = blockLocation.getY();
    final var anchorZ = blockLocation.getZ() + 0.5d;
    final var playerLocation = player.getEyeLocation();

    double x = anchorX + config.getHologramOffsetX();
    final var y = anchorY + config.getHologramOffsetY();
    double z = anchorZ + config.getHologramOffsetZ();
    if (config.getHologramPositionMode() == HologramPositionMode.ROTATIONAL) {
      var forwardX = playerLocation.getX() - anchorX;
      var forwardZ = playerLocation.getZ() - anchorZ;
      final var horizontalLength = Math.hypot(forwardX, forwardZ);
      if (horizontalLength > 1.0e-6d) {
        forwardX /= horizontalLength;
        forwardZ /= horizontalLength;
      } else {
        forwardX = 0d;
        forwardZ = 1d;
      }

      x = anchorX + forwardZ * config.getHologramOffsetX() + forwardX * config.getHologramOffsetZ();
      z = anchorZ - forwardX * config.getHologramOffsetX() + forwardZ * config.getHologramOffsetZ();
    }

    final var toPlayerX = playerLocation.getX() - x;
    final var toPlayerY = playerLocation.getY() - y;
    final var toPlayerZ = playerLocation.getZ() - z;
    final var yaw = (float) Math.toDegrees(Math.atan2(-toPlayerX, toPlayerZ));
    final var pitch = (float) Math.toDegrees(-Math.atan2(toPlayerY, Math.hypot(toPlayerX, toPlayerZ)));
    return new HologramPose(new Vector3d(x, y, z), yaw, pitch);
  }

  private static Component truncate(final Component component, final int maxCodePoints) {
    if (maxCodePoints == 0) return Component.empty();

    final var output = new Component[]{Component.empty()};
    final var remaining = new int[]{maxCodePoints};
    final var styles = new ArrayDeque<Style>();
    final var currentStyle = new Style[]{Style.empty()};
    ComponentFlattener.basic().flatten(component, new FlattenerListener() {
      @Override
      public void pushStyle(final @NonNull Style style) {
        styles.push(currentStyle[0]);
        currentStyle[0] = currentStyle[0].merge(style, Style.Merge.Strategy.ALWAYS);
      }

      @Override
      public void component(final @NonNull String text) {
        if (remaining[0] == 0) return;
        final var codePoints = text.codePointCount(0, text.length());
        final var take = Math.min(remaining[0], codePoints);
        final var endIndex = text.offsetByCodePoints(0, take);
        output[0] = output[0].append(Component.text(text.substring(0, endIndex), currentStyle[0]));
        remaining[0] -= take;
      }

      @Override
      public boolean shouldContinue() {
        return remaining[0] > 0;
      }

      @Override
      public void popStyle(final @NonNull Style style) {
        currentStyle[0] = styles.pop();
      }
    });
    return output[0];
  }

  private record HologramPose(Vector3d position, float yaw, float pitch) {
  }

  private static final class ActiveVisualization {
    private final Block block;
    private final Component song;
    private final AudioTrack track;
    private final Map<UUID, WrappedTask> viewerTasks = new HashMap<>();
    private final Map<UUID, Integer> rotationalTicks = new HashMap<>();
    private WrappedTask particleTask;
    private WrappedTask viewerDiscoveryTask;
    private Integer entityId;

    private ActiveVisualization(final Block block, final Component song, final AudioTrack track) {
      this.block = block;
      this.song = song;
      this.track = track;
    }
  }
}
