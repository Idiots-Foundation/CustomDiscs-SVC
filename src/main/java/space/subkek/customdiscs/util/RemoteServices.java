package space.subkek.customdiscs.util;

import lombok.Getter;
import space.subkek.customdiscs.CustomDiscs;

@Getter
public enum RemoteServices {
  YOUTUBE("youtube"),
  SOUNDCLOUD("soundcloud");

  private final String id;

  RemoteServices(String id) {
    this.id = id;
  }

  public int getCustomModelData() {
    var config = CustomDiscs.getPlugin().getCDConfig();
    return switch (this) {
      case YOUTUBE -> config.getRemoteCustomModelDataYoutube();
      case SOUNDCLOUD -> config.getRemoteCustomModelDataSoundcloud();
    };
  }

  public static RemoteServices fromUrl(String url) {
    if (CustomDiscs.getPlugin().getCDConfig().getRemoteFilterYoutube().stream().anyMatch(url::contains))
      return YOUTUBE;

    if (CustomDiscs.getPlugin().getCDConfig().getRemoteFilterSoundcloud().stream().anyMatch(url::contains))
      return SOUNDCLOUD;

    throw new IllegalArgumentException("Unknown remote service for URL: " + url);
  }
}
