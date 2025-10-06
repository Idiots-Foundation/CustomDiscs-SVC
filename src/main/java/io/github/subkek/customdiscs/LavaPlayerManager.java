package io.github.subkek.customdiscs;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import io.github.subkek.lavaplayer.libs.com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import io.github.subkek.lavaplayer.libs.dev.lavalink.youtube.YoutubeAudioSourceManager;
import io.github.subkek.lavaplayer.libs.dev.lavalink.youtube.YoutubeSourceOptions;
import io.github.subkek.lavaplayer.libs.dev.lavalink.youtube.clients.TvHtml5Embedded;
import io.github.subkek.lavaplayer.libs.dev.lavalink.youtube.clients.Web;
import io.github.subkek.lavaplayer.libs.dev.lavalink.youtube.clients.skeleton.Client;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LavaPlayerManager {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  private final Map<UUID, LavaPlayer> playerMap = new HashMap<>();
  File refreshTokenFile = new File(plugin.getDataFolder(), ".youtube-token");

  private static LavaPlayerManager instance;

  public synchronized static LavaPlayerManager getInstance() {
    if (instance == null) return instance = new LavaPlayerManager();
    return instance;
  }

  public LavaPlayerManager() {
    registerYoutube();
    registerSoundcloud();
  }

  private void registerYoutube() {
    YoutubeSourceOptions options = new YoutubeSourceOptions()
        .setAllowSearch(false);

    if (!plugin.getCDConfig().getYoutubeRemoteServer().isBlank()) {
      options.setRemoteCipherUrl(
          plugin.getCDConfig().getYoutubeRemoteServer(),
          plugin.getCDConfig().getYoutubeRemoteServerPassword()
      );
    }

    YoutubeAudioSourceManager source = getYoutubeAudioSourceManager(options);

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
        CustomDiscs.error("Error load Youtube OAuth2 token: ", e);
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
        new TvHtml5Embedded(),
        new Web()
    };

    CustomDiscs.debug("Registering Youtube Chiper");
    return new YoutubeAudioSourceManager(options, clients);
  }

  private void save() {
    for (AudioSourceManager manager : lavaPlayerManager.getSourceManagers()) {
      if (!(manager instanceof YoutubeAudioSourceManager)) continue;

      CustomDiscs.debug("Found Youtube source to save OAuth2");

      String refreshToken = ((YoutubeAudioSourceManager) manager).getOauth2RefreshToken();
      if (refreshToken == null) continue;

      CustomDiscs.debug("refreshToken not null");

      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(refreshTokenFile));
        writer.write(refreshToken);
        writer.close();
        CustomDiscs.debug("refreshToken written");
      } catch (IOException e) {
        CustomDiscs.error("Error save Youtube OAuth2 token: ", e);
      }
    }
  }

  private void listenForTokenChange(YoutubeAudioSourceManager source) {
    final String currentToken = source.getOauth2RefreshToken() != null
        ? source.getOauth2RefreshToken()
        : "null";

    CustomDiscs.getPlugin().getScheduler().runAtFixedRate(task -> {
      CustomDiscs.debug("Trying to handle token change.");

      String newToken = source.getOauth2RefreshToken();
      if (newToken == null) return;
      if (currentToken.equals(newToken)) return;

      save();
      task.cancel();
    }, 5, 5, TimeUnit.SECONDS);
  }

  public void play(Block block, String ytUrl, Component actionbarComponent) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    CustomDiscs.debug("LavaPlayer UUID is {0}", uuid);
    if (playerMap.containsKey(uuid)) stopPlaying(uuid);

    VoicechatServerApi api = CDVoiceAddon.getInstance().getVoicechatApi();

    LavaPlayer lavaPlayer = new LavaPlayer();
    playerMap.put(uuid, lavaPlayer);

    lavaPlayer.ytUrl = ytUrl;
    lavaPlayer.playerUUID = uuid;
    lavaPlayer.audioChannel = api.createLocationalAudioChannel(
        UUID.randomUUID(),
        api.fromServerLevel(block.getWorld()),
        api.createPosition(
            block.getLocation().getX() + 0.5d,
            block.getLocation().getY() + 0.5d,
            block.getLocation().getZ() + 0.5d
        )
    );

    if (lavaPlayer.audioChannel == null) return;

    lavaPlayer.audioChannel.setCategory(CDVoiceAddon.MUSIC_DISC_CATEGORY);
    lavaPlayer.audioChannel.setDistance(plugin.getCDData().getJukeboxDistance(block));

    lavaPlayer.playersInRange = api.getPlayersInRange(
        api.fromServerLevel(block.getWorld()),
        api.createPosition(
            block.getLocation().getX() + 0.5d,
            block.getLocation().getY() + 0.5d,
            block.getLocation().getZ() + 0.5d
        ),
        plugin.getCDData().getJukeboxDistance(block)
    );

    lavaPlayer.lavaPlayerThread.start();

    for (ServerPlayer serverPlayer : lavaPlayer.playersInRange) {
      Player bukkitPlayer = (Player) serverPlayer.getPlayer();
      bukkitPlayer.sendActionBar(actionbarComponent);
    }
  }

  public void stopPlaying(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    stopPlaying(uuid);
  }

  public void stopPlaying(UUID uuid) {
    if (playerMap.containsKey(uuid)) {
      CustomDiscs.debug(
          "Stopping LavaPlayer {0}",
          uuid.toString());

      LavaPlayer lavaPlayer = playerMap.remove(uuid);

      lavaPlayer.trackFuture.complete(null);
      lavaPlayer.audioPlayer.destroy();
      lavaPlayer.lavaPlayerThread.interrupt();
    } else {
      CustomDiscs.debug(
          "Couldn't find LavaPlayer {0} to stop",
          uuid.toString());
    }
  }

  public void stopPlayingAll() {
    Set.copyOf(playerMap.keySet()).forEach(this::stopPlaying);
  }

  public boolean isPlaying(Block block) {
    UUID id = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    return playerMap.containsKey(id);
  }

  private class LavaPlayer {
    private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();
    private String ytUrl;
    private LocationalAudioChannel audioChannel;
    private Collection<ServerPlayer> playersInRange;
    private UUID playerUUID;
    private AudioPlayer audioPlayer;
    private final Thread lavaPlayerThread = new Thread(this::startTrackJob, "LavaPlayerThread");

    private void startTrackJob() {
      try {
        audioPlayer = lavaPlayerManager.createPlayer();

        lavaPlayerManager.loadItem(ytUrl, new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack audioTrack) {
            CustomDiscs.debug(
                "LavaPlayer {0} loaded track {1} successfully",
                playerUUID.toString(), audioTrack.getInfo().title);
            trackFuture.complete(audioTrack);
          }

          @Override
          public void playlistLoaded(AudioPlaylist audioPlaylist) {
            AudioTrack selected = audioPlaylist.getSelectedTrack();
            CustomDiscs.debug(
                "LavaPlayer {0} loaded track {1} from playlist successfully",
                playerUUID.toString(), selected.getInfo().title);
            trackFuture.complete(selected);
          }

          @Override
          public void noMatches() {
            CustomDiscs.debug(
                "LavaPlayer {0} not found the track {1}",
                playerUUID.toString(), ytUrl);
            for (ServerPlayer serverPlayer : playersInRange) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.no-matches"));
            }
            stopPlaying(playerUUID);
          }

          @Override
          public void loadFailed(FriendlyException e) {
            CustomDiscs.debug(
                "LavaPlayer {0} failed to load the track {1}. Exception msg: {2}",
                playerUUID.toString(), ytUrl, e.getMessage());
            for (ServerPlayer serverPlayer : playersInRange) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.audio-load"));
            }
            stopPlaying(playerUUID);
            trackFuture.completeExceptionally(e);
          }
        });

        if (lavaPlayerThread.isInterrupted()) {
          return;
        }

        AudioTrack audioTrack;
        if (Objects.isNull(audioTrack = trackFuture.get())) {
          CustomDiscs.debug(
              "LavaPlayer {0} excepted track is null, interrupting and return",
              playerUUID.toString());
          if (!lavaPlayerThread.isInterrupted())
            stopPlaying(playerUUID);
          return;
        }

        int volume = Math.round(plugin.getCDConfig().getMusicDiscVolume() * 100);
        audioPlayer.setVolume(volume);

        long start = 0L;

        audioPlayer.playTrack(audioTrack);

        try {
          while (audioPlayer.getPlayingTrack() != null && !lavaPlayerThread.isInterrupted()) {
            if (audioTrack.getState() == AudioTrackState.FINISHED) break;

            AudioFrame frame;
            if (Objects.isNull(frame = audioPlayer.provide(5L, TimeUnit.MILLISECONDS))) continue;

            audioChannel.send(frame.getData());

            if (start == 0L) start = System.currentTimeMillis();
            long wait = (start + frame.getTimecode()) - System.currentTimeMillis();
            if (wait > 0) {
              TimeUnit.MILLISECONDS.sleep(wait);
            }
          }
        } catch (InterruptedException e) {
          CustomDiscs.debug("LavaPlayer {0} got InterruptedException its stop most likely", playerUUID);
          lavaPlayerThread.interrupt();
          return;
        } catch (Throwable e) {
          CustomDiscs.error("LavaPlayer {0} got Throwable Exception: {1}", e, playerUUID);
        }

        stopPlaying(playerUUID);
      } catch (Throwable e) {
        for (ServerPlayer serverPlayer : playersInRange) {
          Player bukkitPlayer = (Player) serverPlayer.getPlayer();
          CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.while-playing"));
          CustomDiscs.error("Error while playing disc: ", e);
        }
      }
    }
  }
}
