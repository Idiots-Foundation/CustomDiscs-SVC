package space.subkek.customdiscs;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LavaPlayerManagerImpl implements LavaPlayerManager {
  private static LavaPlayerManagerImpl instance;

  private static final Pattern PROXY_PATTERN = Pattern.compile(
    "^(?:(https?)://)?(?:(\\w+):(\\w*)@)?([a-zA-Z0-9][a-zA-Z0-9\\-_.]{0,61}|(\\d{1,3}(?:\\.\\d{1,3}){3})):(\\d{1,5})$"
  );

  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  private final Map<UUID, LavaPlayer> playerMap = new ConcurrentHashMap<>();
  private final File refreshTokenFile = new File(plugin.getDataFolder(), ".youtube-token");
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "LavaPlayerExecutorThread"));

  private final List<ActiveHandler> allHandlers = new CopyOnWriteArrayList<>();
  private final Map<Plugin, List<ActiveHandler>> pluginMap = new ConcurrentHashMap<>();

  private final CompletableFuture<Void> initFuture;

  public synchronized static LavaPlayerManagerImpl getInstance() {
    if (instance == null) return instance = new LavaPlayerManagerImpl();
    return instance;
  }

  public LavaPlayerManagerImpl() {
    initFuture = CompletableFuture.runAsync(this::lazyInit, executor);
  }

  private void lazyInit() {
    Consumer<HttpClientBuilder> proxyConfigurator = buildProxyConfigurator();
    if (proxyConfigurator != null) {
      lavaPlayerManager.setHttpBuilderConfigurator(proxyConfigurator);
    }

    registerYoutube(proxyConfigurator);
    registerSoundcloud();
    lavaPlayerManager.registerSourceManager(new LocalAudioSourceManager());

    CustomDiscs.info("LavaPlayer initialized");
  }

  private void registerYoutube(@Nullable Consumer<HttpClientBuilder> proxyConfigurator) {
    YoutubeSourceOptions options = new YoutubeSourceOptions()
      .setAllowSearch(false);

    if (!plugin.getCDConfig().getYoutubeRemoteServer().isBlank()) {
      String pass = plugin.getCDConfig().getYoutubeRemoteServerPassword();
      CustomDiscs.debug("Setting YouTube remote-cipher");
      options.setRemoteCipher(
        plugin.getCDConfig().getYoutubeRemoteServer(),
        pass.isBlank() ? null : pass,
        null
      );
    }

    YoutubeAudioSourceManager source = getYoutubeAudioSourceManager(options);

    if (proxyConfigurator != null) {
      source.getHttpInterfaceManager().configureBuilder(proxyConfigurator);
    }

    if (!plugin.getCDConfig().getYoutubePoToken().isBlank() &&
      !plugin.getCDConfig().getYoutubePoVisitorData().isBlank()) {

      Web.setPoTokenAndVisitorData(
        plugin.getCDConfig().getYoutubePoToken(),
        plugin.getCDConfig().getYoutubePoVisitorData()
      );

    } else if (plugin.getCDConfig().isYoutubeOauth2()) {
      try {
        String oauth2token = null;
        if (refreshTokenFile.exists() && refreshTokenFile.isFile()) {
          oauth2token = Files.readString(refreshTokenFile.toPath()).trim();
        }

        source.useOauth2(oauth2token, false);
        if (oauth2token == null) listenForTokenChange(source);

      } catch (Throwable e) {
        CustomDiscs.error("Failed to load YouTube oauth2 token: ", e);
      }
    }

    lavaPlayerManager.registerSourceManager(source);
  }

  private void registerSoundcloud() {
    SoundCloudAudioSourceManager source = SoundCloudAudioSourceManager.createDefault();
    lavaPlayerManager.registerSourceManager(source);
  }

  private static YoutubeAudioSourceManager getYoutubeAudioSourceManager(YoutubeSourceOptions options) {
    Client[] clients = {
      new Music(),
      new AndroidVr(),
      new Web(),
      new WebEmbedded(),
      new Tv()
    };

    return new YoutubeAudioSourceManager(options, clients);
  }

  private @Nullable Consumer<HttpClientBuilder> buildProxyConfigurator() {
    String proxyString = plugin.getCDConfig().getYoutubeHttpProxy();
    if (proxyString == null || proxyString.isBlank()) return null;

    Matcher matcher = PROXY_PATTERN.matcher(proxyString);
    if (!matcher.matches()) {
      CustomDiscs.error("Failed to parse http-proxy: {}", proxyString);
      return null;
    }

    String scheme = matcher.group(1);
    String username = matcher.group(2);
    String password = matcher.group(3);
    String hostname = matcher.group(4);
    int port = Integer.parseInt(matcher.group(6));

    BasicCredentialsProvider credentials = null;
    if (username != null && !username.isBlank()) {
      credentials = new BasicCredentialsProvider();
      credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password != null ? password : ""));
    }

    HttpHost host = new HttpHost(hostname, port, scheme);
    BasicCredentialsProvider finalCredentials = credentials;

    return builder -> {
      builder.setProxy(host);
      if (finalCredentials != null) {
        builder.setDefaultCredentialsProvider(finalCredentials);
      }
    };
  }

  private synchronized void save() {
    for (AudioSourceManager manager : lavaPlayerManager.getSourceManagers()) {
      if (!(manager instanceof YoutubeAudioSourceManager)) continue;

      CustomDiscs.debug("Found YouTube source to save oauth2 token");

      String refreshToken = ((YoutubeAudioSourceManager) manager).getOauth2RefreshToken();
      if (refreshToken == null) continue;

      CustomDiscs.debug("Oauth2 token is not null");

      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(refreshTokenFile));
        writer.write(refreshToken);
        writer.close();
        CustomDiscs.debug("YouTube's oauth2 token is successfully saved");
      } catch (IOException e) {
        CustomDiscs.error("Failed to save the YouTube's oauth2 token: ", e);
      }
    }
  }

  private void listenForTokenChange(YoutubeAudioSourceManager source) {
    final String currentToken = source.getOauth2RefreshToken() != null
      ? source.getOauth2RefreshToken()
      : "null";

    AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
    ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
      CustomDiscs.debug("Trying to handle token change.");

      String newToken = source.getOauth2RefreshToken();
      if (newToken == null) return;
      if (currentToken.equals(newToken)) return;

      save();
      futureRef.get().cancel(false);
    }, 4, 4, TimeUnit.SECONDS);
    futureRef.set(future);
  }

  @Override
  public void registerPacketHandler(@NotNull Plugin plugin, @NotNull PacketConsumer consumer) {
    ActiveHandler active = new ActiveHandler(plugin, consumer);
    allHandlers.add(active);
    pluginMap.computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>()).add(active);
  }

  @Override
  public void unregisterPacketHandlers(@NotNull Plugin plugin) {
    List<ActiveHandler> handlers = pluginMap.remove(plugin);
    if (handlers != null) {
      allHandlers.removeAll(handlers);
    }
  }

  private void removeHandler(ActiveHandler handler) {
    allHandlers.remove(handler);
    List<ActiveHandler> pluginList = pluginMap.get(handler.plugin);
    if (pluginList != null) pluginList.remove(handler);
  }

  @Override
  public void play(@NotNull Block block, @NotNull String identifier, Component actionbarComponent) {
    UUID uuid = LegacyUtil.getBlockUUID(block);
    if (playerMap.containsKey(uuid)) return;
    CustomDiscs.debug("Starting LavaPlayer: {}", uuid);

    VoicechatServerApi api = CDVoiceAddon.getInstance().getVoicechatApi();
    Position audioPosition = api.createPosition(
      block.getLocation().getX() + 0.5d,
      block.getLocation().getY() + 0.5d,
      block.getLocation().getZ() + 0.5d
    );
    LocationalAudioChannel audioChannel = api.createLocationalAudioChannel(
      UUID.randomUUID(),
      api.fromServerLevel(block.getWorld()),
      audioPosition
    );
    if (audioChannel == null) return;
    audioChannel.setCategory(CDVoiceAddon.MUSIC_DISC_CATEGORY);
    audioChannel.setDistance(plugin.getCDData().getJukeboxDistance(block));

    Collection<ServerPlayer> players = api.getPlayersInRange(
      api.fromServerLevel(block.getWorld()),
      audioPosition,
      plugin.getCDData().getJukeboxDistance(block)
    );

    LavaPlayer lavaPlayer = new LavaPlayer(
      block,
      identifier,
      audioChannel,
      uuid,
      players
    );
    playerMap.put(uuid, lavaPlayer);

    lavaPlayer.lavaPlayerThread.start();

    if (actionbarComponent != null) {
      for (ServerPlayer serverPlayer : lavaPlayer.playersInRangeAtStart) {
        Player bukkitPlayer = (Player) serverPlayer.getPlayer();
        bukkitPlayer.sendActionBar(actionbarComponent);
      }
    }
  }

  @Override
  public void stopPlaying(@NotNull Block block) {
    UUID uuid = LegacyUtil.getBlockUUID(block);
    stopPlaying(uuid);
  }

  private synchronized void stopPlaying(UUID uuid) {
    LavaPlayer lavaPlayer = playerMap.get(uuid);
    if (lavaPlayer != null && lavaPlayer.isRunning) {
      CustomDiscs.debug("Stopping LavaPlayer: {}", uuid);

      CompletableFuture<Void> eventFuture = new CompletableFuture<>();
      executor.execute(() -> {
        try {
          LavaPlayerStopPlayingEvent event = new LavaPlayerStopPlayingEvent(lavaPlayer.block, lavaPlayer.identifier);
          plugin.getServer().getPluginManager().callEvent(event);
        } finally {
          eventFuture.complete(null);
        }
      });
      try {
        eventFuture.get(2, TimeUnit.SECONDS);
      } catch (ExecutionException | InterruptedException | TimeoutException e) {
        CustomDiscs.error("Event timed out for LavaPlayer {}", uuid);
      }

      lavaPlayer.stop();
      playerMap.remove(uuid);
    } else {
      CustomDiscs.debug("LavaPlayer {} already stopped", uuid);
    }
  }

  public synchronized void stopPlayingAll() {
    Set.copyOf(playerMap.keySet()).forEach(this::stopPlaying);
  }

  @Override
  public boolean isPlaying(@NotNull Block block) {
    UUID id = LegacyUtil.getBlockUUID(block);
    return playerMap.containsKey(id);
  }

  @Override
  public @Nullable LocationalAudioChannel getAudioChannel(@NotNull Block block) {
    LavaPlayer lavaPlayer = playerMap.get(LegacyUtil.getBlockUUID(block));
    return lavaPlayer == null ? null : lavaPlayer.audioChannel;
  }

  @Override
  public @Nullable Collection<ServerPlayer> getPlayersInRangeAtStart(@NotNull Block block) {
    LavaPlayer lavaPlayer = playerMap.get(LegacyUtil.getBlockUUID(block));
    return lavaPlayer == null ? null : lavaPlayer.playersInRangeAtStart;
  }

  @SuppressWarnings("ClassCanBeRecord")
  private static class ActiveHandler implements HandlerRegistration {
    private final Plugin plugin;
    private final PacketConsumer consumer;

    private ActiveHandler(Plugin plugin, PacketConsumer consumer) {
      this.plugin = plugin;
      this.consumer = consumer;
    }

    @Override
    public void unregister() {
      LavaPlayerManagerImpl.getInstance().removeHandler(this);
    }
  }

  private class LavaPlayer {
    private final Block block;
    private final String identifier;
    private final LocationalAudioChannel audioChannel;
    private final UUID uuid;
    private final Collection<ServerPlayer> playersInRangeAtStart;

    private final Thread lavaPlayerThread = new Thread(this::threadJob, "LavaPlayerThread");
    private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();

    private AudioPlayer audioPlayer;
    private volatile boolean isRunning = true;

    public LavaPlayer(Block block, String identifier, LocationalAudioChannel audioChannel, UUID uuid, Collection<ServerPlayer> playersInRangeAtStart) {
      this.block = block;
      this.identifier = identifier;
      this.audioChannel = audioChannel;
      this.uuid = uuid;
      this.playersInRangeAtStart = playersInRangeAtStart;
    }

    private void stop() {
      this.isRunning = false;

      lavaPlayerThread.interrupt();
      this.trackFuture.complete(null);
      if (audioPlayer != null)
        this.audioPlayer.destroy();
    }

    private boolean processPacket(Block block, byte[] data) {
      for (ActiveHandler handler : allHandlers) {
        boolean allowed = handler.consumer.process(handler, block, data);
        if (!allowed) return false;
      }
      return true;
    }

    private void threadJob() {
      try {
        initFuture.join();

        var event = new LavaPlayerStartPlayingEvent(this.block, this.identifier);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
          if (isRunning) stopPlaying(uuid);
          return;
        }

        audioPlayer = lavaPlayerManager.createPlayer();

        lavaPlayerManager.loadItem(identifier, new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack audioTrack) {
            CustomDiscs.debug("LavaPlayer {} loaded track {} successfully", uuid, audioTrack.getInfo().title);
            trackFuture.complete(audioTrack);
          }

          @Override
          public void playlistLoaded(AudioPlaylist audioPlaylist) {
            AudioTrack selected = audioPlaylist.getSelectedTrack();
            if (selected == null) {
              selected = audioPlaylist.getTracks().getFirst();
            }

            CustomDiscs.debug("LavaPlayer {} loaded track {} from playlist successfully", uuid, selected.getInfo().title);
            trackFuture.complete(selected);
          }

          @Override
          public void noMatches() {
            CustomDiscs.debug("LavaPlayer {} didn't found the track {}", uuid, identifier);
            for (ServerPlayer serverPlayer : playersInRangeAtStart) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.no-matches"));
            }
            if (isRunning) stopPlaying(uuid);
          }

          @Override
          public void loadFailed(FriendlyException e) {
            CustomDiscs.debug("LavaPlayer {} failed to load the track {}: {}", uuid, identifier, e.getMessage());
            for (ServerPlayer serverPlayer : playersInRangeAtStart) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.audio-load"));
            }
            if (isRunning) stopPlaying(uuid);
            trackFuture.complete(null);
          }
        });


        AudioTrack audioTrack;
        try {
          audioTrack = trackFuture.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          audioTrack = null;
          lavaPlayerThread.interrupt();
          CustomDiscs.debug("LavaPlayer {} got interrupt while loading", uuid);
        }

        if (audioTrack == null) {
          CustomDiscs.debug("LavaPlayer {} expected track is null. Stopping...", uuid);
          if (isRunning) stopPlaying(uuid);
          return;
        }

        int volume = Math.round(plugin.getCDConfig().getMusicDiscVolume() * 100);
        audioPlayer.setVolume(volume);
        audioPlayer.playTrack(audioTrack);

        try {
          long start = System.currentTimeMillis();
          while (isRunning && !lavaPlayerThread.isInterrupted() && audioPlayer.getPlayingTrack() != null && audioTrack.getState() != AudioTrackState.FINISHED) {
            AudioFrame frame = audioPlayer.provide(20L, TimeUnit.MILLISECONDS);
            if (frame == null) {
              TimeUnit.MILLISECONDS.sleep(50);
              continue;
            }

            byte[] data = frame.getData();
            if (processPacket(this.block, data))
              audioChannel.send(frame.getData());

            long wait = (start + frame.getTimecode()) - System.currentTimeMillis();
            if (wait > 0) TimeUnit.MILLISECONDS.sleep(wait);
          }
        } catch (InterruptedException e) {
          CustomDiscs.debug("LavaPlayer {} got interrupt", uuid);
          Thread.currentThread().interrupt();
        } catch (Throwable e) {
          CustomDiscs.error("LavaPlayer {} got unexcepted exception: {}", e, uuid);
        }

        if (isRunning) stopPlaying(uuid);
      } catch (Throwable e) {
        for (ServerPlayer serverPlayer : playersInRangeAtStart) {
          Player bukkitPlayer = (Player) serverPlayer.getPlayer();
          CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.while-playing"));
          CustomDiscs.error("LavaPlayer {} got exception: ", e, uuid);
        }
      }
    }
  }
}
