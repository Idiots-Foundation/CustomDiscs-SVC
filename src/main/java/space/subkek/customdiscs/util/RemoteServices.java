package space.subkek.customdiscs.util;

import lombok.Getter;
import space.subkek.customdiscs.CustomDiscs;
import space.subkek.customdiscs.file.CDConfig;

import java.util.List;
import java.util.function.Function;

@Getter
public enum RemoteServices {
  YOUTUBE("youtube", CDConfig::getRemoteFilterYoutube, CDConfig::getRemoteCustomModelDataYoutube),
  SOUNDCLOUD("soundcloud", CDConfig::getRemoteFilterSoundcloud, CDConfig::getRemoteCustomModelDataSoundcloud);

  private final String id;
  private final Function<CDConfig, List<String>> filterProvider;
  private final Function<CDConfig, Integer> modelDataProvider;

  RemoteServices(String id, Function<CDConfig, List<String>> filterProvider,
                 Function<CDConfig, Integer> modelDataProvider) {
    this.id = id;
    this.filterProvider = filterProvider;
    this.modelDataProvider = modelDataProvider;
  }

  public int getCustomModelData() {
    return modelDataProvider.apply(CustomDiscs.getPlugin().getCDConfig());
  }

  public static RemoteServices fromUrl(String url) {
    CDConfig config = CustomDiscs.getPlugin().getCDConfig();

    for (RemoteServices service : values()) {
      if (matchesAny(url, service.filterProvider.apply(config))) {
        return service;
      }
    }

    return null;
  }

  private static boolean matchesAny(String url, List<String> patterns) {
    return patterns.stream().anyMatch(url::contains);
  }
}