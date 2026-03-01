package space.subkek.customdiscs.util;

import lombok.Getter;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.file.CDConfig;

import java.util.function.Function;
import java.util.regex.Pattern;

@Getter
public enum RemoteServices {
  YOUTUBE("youtube", CDConfig::getRemoteFilterYoutube, CDConfig::getRemoteCustomModelDataYoutube),
  SOUNDCLOUD("soundcloud", CDConfig::getRemoteFilterSoundcloud, CDConfig::getRemoteCustomModelDataSoundcloud),
  DEEZER("deezer", CDConfig::getRemoteFilterDeezer, CDConfig::getRemoteCustomModelDataDeezer);

  private final String id;
  private final Function<CDConfig, String> filterProvider;
  private final Function<CDConfig, Integer> modelDataProvider;

  RemoteServices(String id, Function<CDConfig, String> filterProvider, Function<CDConfig, Integer> modelDataProvider) {
    this.id = id;
    this.filterProvider = filterProvider;
    this.modelDataProvider = modelDataProvider;
  }

  public int getCustomModelData() {
    return modelDataProvider.apply(CustomDiscs.getPlugin().getCDConfig());
  }

  public static RemoteServices fromUrl(String url) {
    for (RemoteServices service : values()) {
      if (matchesAny(url, service.filterProvider.apply(CustomDiscs.getPlugin().getCDConfig()))) {
        return service;
      }
    }
    return null;
  }

  private static boolean matchesAny(String url, String regex) {
    return Pattern.compile(regex).matcher(url).find();
  }
}
