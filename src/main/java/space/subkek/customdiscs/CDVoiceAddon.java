package space.subkek.customdiscs;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class CDVoiceAddon implements VoicechatPlugin {
  public static final String MUSIC_DISC_CATEGORY = "music_discs";
  private static CDVoiceAddon instance;
  private VoicechatServerApi voicechatApi;
  private VolumeCategory musicDiscsCategory;

  public static synchronized CDVoiceAddon getInstance() {
    if (instance == null) return instance = new CDVoiceAddon();
    return instance;
  }

  @Override
  public String getPluginId() {
    return CustomDiscs.getPlugin().getName().toLowerCase();
  }

  @Override
  public void initialize(final VoicechatApi api) {
    this.voicechatApi = (VoicechatServerApi) api;
  }

  @Override
  public void registerEvents(final EventRegistration registration) {
    registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
      this.musicDiscsCategory = this.voicechatApi.volumeCategoryBuilder()
        .setId(MUSIC_DISC_CATEGORY)
        .setName("Music Discs")
        .setIcon(this.getMusicDiscIcon())
        .build();
      this.voicechatApi.registerVolumeCategory(this.musicDiscsCategory);
    });
  }

  private int[][] getMusicDiscIcon() {
    try {
      final var resources = this.getClass().getClassLoader().getResources("music_disc_category.png");

      while (resources.hasMoreElements()) {
        final var bufferedImage = ImageIO.read(resources.nextElement().openStream());
        if (bufferedImage.getWidth() != 16) {
          continue;
        }
        if (bufferedImage.getHeight() != 16) {
          continue;
        }
        final var image = new int[16][16];
        for (var x = 0; x < bufferedImage.getWidth(); x++) {
          for (var y = 0; y < bufferedImage.getHeight(); y++) {
            image[x][y] = bufferedImage.getRGB(x, y);
          }
        }
        return image;
      }
    } catch (final Throwable e) {
      CustomDiscs.error("Error getting music discs icon: ", e);
    }
    return null;
  }
}
