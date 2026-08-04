package space.subkek.customdiscs.util;

import lombok.Getter;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.file.CDConfig;

import java.util.function.Function;
import java.util.regex.Pattern;

@Getter
public enum RemoteServices {
  YOUTUBE("youtube", CDConfig::getRemoteFilterYoutube, CDConfig::getRemoteCustomModelDataYoutube),
  SOUNDCLOUD("soundcloud", CDConfig::getRemoteFilterSoundcloud, CDConfig::getRemoteCustomModelDataSoundcloud);

  private final String id;
  private final Function<CDConfig, String> filterProvider;
  private final Function<CDConfig, Integer> modelDataProvider;

  RemoteServices(final String id, final Function<CDConfig, String> filterProvider, final Function<CDConfig, Integer> modelDataProvider) {
    this.id = id;
    this.filterProvider = filterProvider;
    this.modelDataProvider = modelDataProvider;
  }

  public static RemoteServices fromUrl(final String url) {
    for (final var service : values()) {
      if (matchesAny(url, service.filterProvider.apply(CustomDiscs.getPlugin().getCDConfig()))) {
        return service;
      }
    }
    return null;
  }

  private static boolean matchesAny(final String url, final String regex) {
    return Pattern.compile(regex).matcher(url).find();
  }

  public int getCustomModelData() {
    return this.modelDataProvider.apply(CustomDiscs.getPlugin().getCDConfig());
  }
}
