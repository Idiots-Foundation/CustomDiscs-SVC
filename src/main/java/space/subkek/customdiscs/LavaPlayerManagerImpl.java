package space.subkek.customdiscs;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.*;
import dev.lavalink.youtube.clients.skeleton.Client;
import net.kyori.adventure.text.Component;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.subkek.customdiscs.api.LavaPlayerManager;
import space.subkek.customdiscs.api.event.LavaPlayerStartPlayingEvent;
import space.subkek.customdiscs.api.event.LavaPlayerStopPlayingEvent;
import space.subkek.customdiscs.util.LegacyUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class LavaPlayerManagerImpl implements LavaPlayerManager {
  private static final Pattern PROXY_PATTERN = Pattern.compile(
    "^(?:(https?)://)?(?:(\\w+):(\\w*)@)?([a-zA-Z0-9][a-zA-Z0-9\\-_.]{0,61}|(\\d{1,3}(?:\\.\\d{1,3}){3})):(\\d{1,5})$"
  );
  private static LavaPlayerManagerImpl instance;
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  private final Map<UUID, LavaPlayer> playerMap = new ConcurrentHashMap<>();
  private final File refreshTokenFile = new File(this.plugin.getDataFolder(), ".youtube-token");
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "LavaPlayerExecutorThread"));

  private final List<ActiveHandler> allHandlers = new CopyOnWriteArrayList<>();
  private final Map<Plugin, List<ActiveHandler>> pluginMap = new ConcurrentHashMap<>();

  private final CompletableFuture<Void> initFuture;

  public LavaPlayerManagerImpl() {
    this.initFuture = CompletableFuture.runAsync(this::lazyInit, this.executor);
  }

  public static synchronized LavaPlayerManagerImpl getInstance() {
    if (instance == null) return instance = new LavaPlayerManagerImpl();
    return instance;
  }

  private static YoutubeAudioSourceManager getYoutubeAudioSourceManager(final YoutubeSourceOptions options) {
    final Client[] clients = {
      new Music(),
      new AndroidVr(),
      new Web(),
      new WebEmbedded(),
      new Tv()
    };

    return new YoutubeAudioSourceManager(options, clients);
  }

  private void lazyInit() {
    final var proxyConfigurator = this.buildProxyConfigurator();
    if (proxyConfigurator != null) {
      this.lavaPlayerManager.setHttpBuilderConfigurator(proxyConfigurator);
    }

    this.registerYoutube(proxyConfigurator);
    this.registerSoundcloud();
    this.lavaPlayerManager.registerSourceManager(new LocalAudioSourceManager());

    CustomDiscs.info("LavaPlayer initialized");
  }

  private void registerYoutube(@Nullable final Consumer<HttpClientBuilder> proxyConfigurator) {
    final var options = new YoutubeSourceOptions()
      .setAllowSearch(false);

    if (!this.plugin.getCDConfig().getYoutubeRemoteServer().isBlank()) {
      final var pass = this.plugin.getCDConfig().getYoutubeRemoteServerPassword();
      CustomDiscs.debug("Setting YouTube remote-cipher");
      options.setRemoteCipher(
        this.plugin.getCDConfig().getYoutubeRemoteServer(),
        pass.isBlank() ? null : pass,
        null
      );
    }

    final var source = getYoutubeAudioSourceManager(options);

    if (proxyConfigurator != null) {
      source.getHttpInterfaceManager().configureBuilder(proxyConfigurator);
    }

    if (!this.plugin.getCDConfig().getYoutubePoToken().isBlank() &&
      !this.plugin.getCDConfig().getYoutubePoVisitorData().isBlank()) {

      Web.setPoTokenAndVisitorData(
        this.plugin.getCDConfig().getYoutubePoToken(),
        this.plugin.getCDConfig().getYoutubePoVisitorData()
      );

    } else if (this.plugin.getCDConfig().isYoutubeOauth2()) {
      try {
        String oauth2token = null;
        if (this.refreshTokenFile.exists() && this.refreshTokenFile.isFile()) {
          oauth2token = Files.readString(this.refreshTokenFile.toPath()).trim();
        }

        source.useOauth2(oauth2token, false);
        if (oauth2token == null) this.listenForTokenChange(source);

      } catch (final Throwable e) {
        CustomDiscs.error("Failed to load YouTube oauth2 token: ", e);
      }
    }

    this.lavaPlayerManager.registerSourceManager(source);
  }

  private void registerSoundcloud() {
    final var source = SoundCloudAudioSourceManager.createDefault();
    this.lavaPlayerManager.registerSourceManager(source);
  }

  private @Nullable Consumer<HttpClientBuilder> buildProxyConfigurator() {
    final var proxyString = this.plugin.getCDConfig().getYoutubeHttpProxy();
    if (proxyString == null || proxyString.isBlank()) return null;

    final var matcher = PROXY_PATTERN.matcher(proxyString);
    if (!matcher.matches()) {
      CustomDiscs.error("Failed to parse http-proxy: {}", proxyString);
      return null;
    }

    final var scheme = matcher.group(1);
    final var username = matcher.group(2);
    final var password = matcher.group(3);
    final var hostname = matcher.group(4);
    final var port = Integer.parseInt(matcher.group(6));

    BasicCredentialsProvider credentials = null;
    if (username != null && !username.isBlank()) {
      credentials = new BasicCredentialsProvider();
      credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password != null ? password : ""));
    }

    final var host = new HttpHost(hostname, port, scheme);
    final var finalCredentials = credentials;

    return builder -> {
      builder.setProxy(host);
      if (finalCredentials != null) {
        builder.setDefaultCredentialsProvider(finalCredentials);
      }
    };
  }

  private synchronized void save() {
    for (final var manager : this.lavaPlayerManager.getSourceManagers()) {
      if (!(manager instanceof YoutubeAudioSourceManager)) continue;

      CustomDiscs.debug("Found YouTube source to save oauth2 token");

      final var refreshToken = ((YoutubeAudioSourceManager) manager).getOauth2RefreshToken();
      if (refreshToken == null) continue;

      CustomDiscs.debug("Oauth2 token is not null");

      try {
        final var writer = new BufferedWriter(new FileWriter(this.refreshTokenFile));
        writer.write(refreshToken);
        writer.close();
        CustomDiscs.debug("YouTube's oauth2 token is successfully saved");
      } catch (final IOException e) {
        CustomDiscs.error("Failed to save the YouTube's oauth2 token: ", e);
      }
    }
  }

  private void listenForTokenChange(final YoutubeAudioSourceManager source) {
    final var currentToken = source.getOauth2RefreshToken() != null
      ? source.getOauth2RefreshToken()
      : "null";

    final var futureRef = new AtomicReference<ScheduledFuture<?>>();
    final var future = this.executor.scheduleAtFixedRate(() -> {
      CustomDiscs.debug("Trying to handle token change.");

      final var newToken = source.getOauth2RefreshToken();
      if (newToken == null) return;
      if (currentToken.equals(newToken)) return;

      this.save();
      futureRef.get().cancel(false);
    }, 4, 4, TimeUnit.SECONDS);
    futureRef.set(future);
  }

  @Override
  public void registerPacketHandler(@NotNull final Plugin plugin, @NotNull final PacketConsumer consumer) {
    final var active = new ActiveHandler(plugin, consumer);
    this.allHandlers.add(active);
    this.pluginMap.computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>()).add(active);
  }

  @Override
  public void unregisterPacketHandlers(@NotNull final Plugin plugin) {
    final var handlers = this.pluginMap.remove(plugin);
    if (handlers != null) {
      this.allHandlers.removeAll(handlers);
    }
  }

  private void removeHandler(final ActiveHandler handler) {
    this.allHandlers.remove(handler);
    final var pluginList = this.pluginMap.get(handler.plugin);
    if (pluginList != null) pluginList.remove(handler);
  }

  @Override
  public void play(@NotNull final Block block, @NotNull final String identifier, final Component actionbarComponent) {
    this.playDisc(block, identifier, actionbarComponent, actionbarComponent, BlockFace.SOUTH);
  }

  public void playDisc(@NotNull final Block block, @NotNull final String identifier,
                       final Component actionbarComponent, final Component songComponent,
                       @NotNull final BlockFace hologramFacing) {
    final var uuid = LegacyUtil.getBlockUUID(block);
    if (this.playerMap.containsKey(uuid)) return;
    CustomDiscs.debug("Starting LavaPlayer: {}", uuid);

    final var api = CDVoiceAddon.getInstance().getVoicechatApi();
    final var audioPosition = api.createPosition(
      block.getLocation().getX() + 0.5d,
      block.getLocation().getY() + 0.5d,
      block.getLocation().getZ() + 0.5d
    );
    final var audioChannel = api.createLocationalAudioChannel(
      UUID.randomUUID(),
      api.fromServerLevel(block.getWorld()),
      audioPosition
    );
    if (audioChannel == null) return;
    audioChannel.setCategory(CDVoiceAddon.MUSIC_DISC_CATEGORY);
    audioChannel.setDistance(this.plugin.getCDData().getJukeboxDistance(block));

    final var players = api.getPlayersInRange(
      api.fromServerLevel(block.getWorld()),
      audioPosition,
      this.plugin.getCDData().getJukeboxDistance(block)
    );

    if (actionbarComponent != null) {
      for (final var serverPlayer : players) {
        final var bukkitPlayer = (Player) serverPlayer.getPlayer();
        this.plugin.getFoliaLib().getScheduler().runAtEntity(
          bukkitPlayer,
          task -> bukkitPlayer.sendActionBar(actionbarComponent)
        );
      }
    }

    final var lavaPlayer = new LavaPlayer(
      block,
      identifier,
      audioChannel,
      uuid,
      players,
      songComponent != null ? songComponent : Component.empty(),
      hologramFacing
    );
    this.playerMap.put(uuid, lavaPlayer);

    lavaPlayer.lavaPlayerThread.start();
  }

  @Override
  public void stopPlaying(@NotNull final Block block) {
    final var uuid = LegacyUtil.getBlockUUID(block);
    this.stopPlaying(uuid);
  }

  private synchronized void stopPlaying(final UUID uuid) {
    final var lavaPlayer = this.playerMap.get(uuid);
    if (lavaPlayer != null && lavaPlayer.isRunning) {
      CustomDiscs.debug("Stopping LavaPlayer: {}", uuid);

      final var eventFuture = new CompletableFuture<Void>();
      this.executor.execute(() -> {
        try {
          final var event = new LavaPlayerStopPlayingEvent(lavaPlayer.block, lavaPlayer.identifier);
          this.plugin.getServer().getPluginManager().callEvent(event);
        } finally {
          eventFuture.complete(null);
        }
      });
      try {
        eventFuture.get(2, TimeUnit.SECONDS);
      } catch (final ExecutionException | InterruptedException | TimeoutException e) {
        CustomDiscs.error("Event timed out for LavaPlayer {}", uuid);
      }

      lavaPlayer.stop();
      this.playerMap.remove(uuid);
    } else {
      CustomDiscs.debug("LavaPlayer {} already stopped", uuid);
    }
  }

  public synchronized void stopPlayingAll() {
    Set.copyOf(this.playerMap.keySet()).forEach(this::stopPlaying);
  }

  @Override
  public boolean isPlaying(@NotNull final Block block) {
    final var id = LegacyUtil.getBlockUUID(block);
    return this.playerMap.containsKey(id);
  }

  @Override
  public @Nullable LocationalAudioChannel getAudioChannel(@NotNull final Block block) {
    final var lavaPlayer = this.playerMap.get(LegacyUtil.getBlockUUID(block));
    return lavaPlayer == null ? null : lavaPlayer.audioChannel;
  }

  @Override
  public @Nullable Collection<ServerPlayer> getPlayersInRangeAtStart(@NotNull final Block block) {
    final var lavaPlayer = this.playerMap.get(LegacyUtil.getBlockUUID(block));
    return lavaPlayer == null ? null : lavaPlayer.playersInRangeAtStart;
  }

  @SuppressWarnings("ClassCanBeRecord")
  private static final class ActiveHandler implements HandlerRegistration {
    private final Plugin plugin;
    private final PacketConsumer consumer;

    private ActiveHandler(final Plugin plugin, final PacketConsumer consumer) {
      this.plugin = plugin;
      this.consumer = consumer;
    }

    @Override
    public void unregister() {
      LavaPlayerManagerImpl.getInstance().removeHandler(this);
    }
  }

  private final class LavaPlayer {
    private final Block block;
    private final String identifier;
    private final LocationalAudioChannel audioChannel;
    private final UUID uuid;
    private final Collection<ServerPlayer> playersInRangeAtStart;
    private final Component songComponent;
    private final BlockFace hologramFacing;
    private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();
    private final Thread lavaPlayerThread = new Thread(this::threadJob, "LavaPlayerThread");
    private AudioPlayer audioPlayer;
    private volatile boolean isRunning = true;

    public LavaPlayer(final Block block, final String identifier, final LocationalAudioChannel audioChannel,
                      final UUID uuid, final Collection<ServerPlayer> playersInRangeAtStart,
                      final Component songComponent, final BlockFace hologramFacing) {
      this.block = block;
      this.identifier = identifier;
      this.audioChannel = audioChannel;
      this.uuid = uuid;
      this.playersInRangeAtStart = playersInRangeAtStart;
      this.songComponent = songComponent;
      this.hologramFacing = hologramFacing;
    }

    private void stop() {
      this.isRunning = false;
      LavaPlayerManagerImpl.this.plugin.getVisualizationManager().stop(this.block);

      this.lavaPlayerThread.interrupt();
      this.trackFuture.complete(null);
      if (this.audioPlayer != null)
        this.audioPlayer.destroy();
    }

    private boolean processPacket(final Block block, final byte[] data) {
      for (final var handler : LavaPlayerManagerImpl.this.allHandlers) {
        final var allowed = handler.consumer.process(handler, block, data);
        if (!allowed) return false;
      }
      return true;
    }

    private void threadJob() {
      try {
        LavaPlayerManagerImpl.this.initFuture.join();

        final var event = new LavaPlayerStartPlayingEvent(this.block, this.identifier);
        LavaPlayerManagerImpl.this.plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
          if (this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(this.uuid);
          return;
        }

        this.audioPlayer = LavaPlayerManagerImpl.this.lavaPlayerManager.createPlayer();

        LavaPlayerManagerImpl.this.lavaPlayerManager.loadItem(this.identifier, new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(final AudioTrack audioTrack) {
            CustomDiscs.debug("LavaPlayer {} loaded track {} successfully", LavaPlayer.this.uuid, audioTrack.getInfo().title);
            LavaPlayer.this.trackFuture.complete(audioTrack);
          }

          @Override
          public void playlistLoaded(final AudioPlaylist audioPlaylist) {
            var selected = audioPlaylist.getSelectedTrack();
            if (selected == null) {
              selected = audioPlaylist.getTracks().getFirst();
            }

            CustomDiscs.debug("LavaPlayer {} loaded track {} from playlist successfully", LavaPlayer.this.uuid, selected.getInfo().title);
            LavaPlayer.this.trackFuture.complete(selected);
          }

          @Override
          public void noMatches() {
            CustomDiscs.debug("LavaPlayer {} didn't found the track {}", LavaPlayer.this.uuid, LavaPlayer.this.identifier);
            for (final var serverPlayer : LavaPlayer.this.playersInRangeAtStart) {
              final var bukkitPlayer = (Player) serverPlayer.getPlayer();
              CustomDiscs.sendMessage(bukkitPlayer, LavaPlayerManagerImpl.this.plugin.getLanguage().PComponent("error.play.no-matches"));
            }
            if (LavaPlayer.this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(LavaPlayer.this.uuid);
          }

          @Override
          public void loadFailed(final FriendlyException e) {
            CustomDiscs.debug("LavaPlayer {} failed to load the track {}: {}", LavaPlayer.this.uuid, LavaPlayer.this.identifier, e.getMessage());
            for (final var serverPlayer : LavaPlayer.this.playersInRangeAtStart) {
              final var bukkitPlayer = (Player) serverPlayer.getPlayer();
              CustomDiscs.sendMessage(bukkitPlayer, LavaPlayerManagerImpl.this.plugin.getLanguage().PComponent("error.play.audio-load"));
            }
            if (LavaPlayer.this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(LavaPlayer.this.uuid);
            LavaPlayer.this.trackFuture.complete(null);
          }
        });


        AudioTrack audioTrack;
        try {
          audioTrack = this.trackFuture.get(30, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
          audioTrack = null;
          this.lavaPlayerThread.interrupt();
          CustomDiscs.debug("LavaPlayer {} got interrupt while loading", this.uuid);
        }

        if (audioTrack == null) {
          CustomDiscs.debug("LavaPlayer {} expected track is null. Stopping...", this.uuid);
          if (this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(this.uuid);
          return;
        }

        final var maxTrackLengthSeconds = LavaPlayerManagerImpl.this.plugin.getCDConfig().getMaxTrackLengthSeconds();
        if (maxTrackLengthSeconds > 0) {
          final var duration = audioTrack.getDuration();
          if (audioTrack.getInfo().isStream || duration <= 0L || duration == Units.DURATION_MS_UNKNOWN) {
            this.sendLocalizedError("error.play.unknown-track-length");
            if (this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(this.uuid);
            return;
          }
          if (duration > maxTrackLengthSeconds * 1000L) {
            this.sendLocalizedError("error.play.track-too-long");
            if (this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(this.uuid);
            return;
          }
        }

        final var volume = Math.round(LavaPlayerManagerImpl.this.plugin.getCDConfig().getMusicDiscVolume() * 100);
        this.audioPlayer.setVolume(volume);
        this.audioPlayer.playTrack(audioTrack);
        this.startVisualization(audioTrack);

        try {
          final var start = System.currentTimeMillis();
          while (this.isRunning && !this.lavaPlayerThread.isInterrupted() && this.audioPlayer.getPlayingTrack() != null && audioTrack.getState() != AudioTrackState.FINISHED) {
            final var frame = this.audioPlayer.provide(20L, TimeUnit.MILLISECONDS);
            if (frame == null) {
              TimeUnit.MILLISECONDS.sleep(50);
              continue;
            }

            final var data = frame.getData();
            if (this.processPacket(this.block, data))
              this.audioChannel.send(frame.getData());

            final var wait = (start + frame.getTimecode()) - System.currentTimeMillis();
            if (wait > 0) TimeUnit.MILLISECONDS.sleep(wait);
          }
        } catch (final InterruptedException e) {
          CustomDiscs.debug("LavaPlayer {} got interrupt", this.uuid);
          Thread.currentThread().interrupt();
        } catch (final Throwable e) {
          CustomDiscs.error("LavaPlayer {} got unexcepted exception: {}", e, this.uuid);
        }

        if (this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(this.uuid);
      } catch (final Throwable e) {
        for (final var serverPlayer : this.playersInRangeAtStart) {
          final var bukkitPlayer = (Player) serverPlayer.getPlayer();
          CustomDiscs.sendMessage(bukkitPlayer, LavaPlayerManagerImpl.this.plugin.getLanguage().PComponent("error.play.while-playing"));
          CustomDiscs.error("LavaPlayer {} got exception: ", e, this.uuid);
        }
        if (this.isRunning) LavaPlayerManagerImpl.this.stopPlaying(this.uuid);
      }
    }

    private void startVisualization(final AudioTrack audioTrack) {
      LavaPlayerManagerImpl.this.plugin.getFoliaLib().getScheduler().runAtLocation(
        this.block.getLocation(),
        task -> {
          if (!this.isRunning) return;
          LavaPlayerManagerImpl.this.plugin.getVisualizationManager().start(
            this.block,
            this.songComponent,
            audioTrack,
            this.hologramFacing
          );
        }
      );
    }

    private void sendLocalizedError(final String key) {
      for (final var serverPlayer : this.playersInRangeAtStart) {
        final var bukkitPlayer = (Player) serverPlayer.getPlayer();
        LavaPlayerManagerImpl.this.plugin.getFoliaLib().getScheduler().runAtEntity(
          bukkitPlayer,
          task -> CustomDiscs.sendMessage(bukkitPlayer, LavaPlayerManagerImpl.this.plugin.getLanguage().PComponent(key))
        );
      }
    }


  }
}
